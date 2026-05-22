import Foundation
import SwiftUI
import UIKit
import Shared

enum IosProcessingState {
    case idle
    case queued
    case processing
    case completed
    case failed
}

@MainActor
final class PluctViewModel: ObservableObject {
    @Published var urlText = ""
    @Published var apiEnvironment = "https://pluct-business-engine.romeo-lya2.workers.dev"
    @Published var walletLabel = "Credits: --"
    @Published var transcript = ""
    @Published var diagnosticsText = "Ready"
    @Published var processingState: IosProcessingState = .idle
    @Published var isBusy = false
    @Published var validationMessage: String?
    @Published var isCurrentUrlValid = false

    let deviceId: String
    let appVersion: String

    private var lastSanitizedUrl: String?

    init() {
        let defaults = UserDefaults.standard
        if let existing = defaults.string(forKey: "pluct.deviceId") {
            deviceId = existing
        } else {
            let generated = "ios-\(UUID().uuidString.lowercased())"
            defaults.set(generated, forKey: "pluct.deviceId")
            deviceId = generated
        }
        appVersion = Bundle.main.infoDictionary?["CFBundleShortVersionString"] as? String ?? "1.0.1"
        transcript = defaults.string(forKey: "pluct.lastTranscript") ?? ""
    }

    func pasteFromClipboard() {
        if let text = UIPasteboard.general.string {
            urlText = text
            validateCurrentUrl()
        }
    }

    func startTranscription() async {
        validateCurrentUrl()
        guard isCurrentUrlValid, let sanitizedUrl = lastSanitizedUrl else { return }

        isBusy = true
        processingState = .queued
        diagnosticsText = "Queued \(sanitizedUrl)"

        do {
            try await Task.sleep(nanoseconds: 300_000_000)
            processingState = .processing
            let requestId = PluctRequestIds.shared.generate(prefix: "ios")
            let request = TranscriptionRequest(url: sanitizedUrl, clientRequestId: requestId)
            diagnosticsText = "Request \(request.clientRequestId)\nSubmitting \(request.url)"

            let result = try await submitAndPoll(request: request)
            transcript = result
            UserDefaults.standard.set(result, forKey: "pluct.lastTranscript")
            processingState = .completed
            diagnosticsText = "Completed \(request.clientRequestId)"
        } catch {
            processingState = .failed
            let mapped = PluctApiErrorMapper.shared.map(statusCode: nil, message: error.localizedDescription)
            diagnosticsText = "\(mapped.category): \(mapped.userMessage)\n\(error.localizedDescription)"
        }

        isBusy = false
    }

    func retry() async {
        await startTranscription()
    }

    func copyTranscript() {
        UIPasteboard.general.string = transcript
        diagnosticsText = "Transcript copied"
    }

    private func validateCurrentUrl() {
        let result = PluctTikTokUrlValidator.shared.validateUrl(url: urlText)
        isCurrentUrlValid = result.isValid
        lastSanitizedUrl = result.isValid ? result.sanitizedValue : nil
        validationMessage = result.isValid ? "TikTok link ready" : result.errorMessage
    }

    private func submitAndPoll(request: TranscriptionRequest) async throws -> String {
        var urlRequest = URLRequest(url: URL(string: "\(apiEnvironment.trimmingCharacters(in: CharacterSet(charactersIn: "/")))/ttt/transcribe")!)
        urlRequest.httpMethod = "POST"
        urlRequest.setValue("application/json", forHTTPHeaderField: "Content-Type")
        urlRequest.setValue(request.clientRequestId, forHTTPHeaderField: "X-Request-ID")
        urlRequest.httpBody = try JSONSerialization.data(withJSONObject: [
            "url": request.url,
            "clientRequestId": request.clientRequestId,
            "deviceId": deviceId,
            "platform": "ios"
        ])

        let (data, response) = try await URLSession.shared.data(for: urlRequest)
        guard let http = response as? HTTPURLResponse else {
            throw PluctIosError.invalidResponse
        }
        guard (200...299).contains(http.statusCode) else {
            throw PluctIosError.http(http.statusCode, String(data: data, encoding: .utf8) ?? "")
        }

        let submitJson = try JSONSerialization.jsonObject(with: data) as? [String: Any]
        guard let jobId = submitJson?["jobId"] as? String else {
            throw PluctIosError.missingJobId
        }

        for attempt in 1...30 {
            let delayMs = PluctRetryPolicy.shared.calculateRetryDelayMs(attemptNumber: min(attempt, 3), baseDelayMs: 1000, maxDelayMs: 3000)
            try await Task.sleep(nanoseconds: UInt64(delayMs) * 1_000_000)

            let pollUrl = URL(string: "\(apiEnvironment.trimmingCharacters(in: CharacterSet(charactersIn: "/")))/ttt/poll/\(jobId)")!
            let (pollData, pollResponse) = try await URLSession.shared.data(from: pollUrl)
            guard let pollHttp = pollResponse as? HTTPURLResponse else {
                throw PluctIosError.invalidResponse
            }
            guard (200...299).contains(pollHttp.statusCode) else {
                throw PluctIosError.http(pollHttp.statusCode, String(data: pollData, encoding: .utf8) ?? "")
            }

            let pollJson = try JSONSerialization.jsonObject(with: pollData) as? [String: Any]
            let status = pollJson?["status"] as? String
            let normalized = PluctTranscriptionState.shared.normalizeStatus(status: status)
            diagnosticsText = "Job \(jobId)\nStatus \(status ?? "unknown")\nAttempt \(attempt)"

            if normalized.name == "COMPLETED" {
                if let transcript = pollJson?["transcript"] as? String ?? pollJson?["text"] as? String {
                    return transcript
                }
                if let result = pollJson?["result"] as? [String: Any],
                   let nested = result["transcript"] as? String ?? result["text"] as? String ?? result["transcription"] as? String {
                    return nested
                }
                throw PluctIosError.missingTranscript
            }
            if normalized.name == "FAILED" {
                throw PluctIosError.remoteFailed
            }
        }

        throw PluctIosError.timeout
    }
}

enum PluctIosError: LocalizedError {
    case invalidResponse
    case http(Int, String)
    case missingJobId
    case missingTranscript
    case remoteFailed
    case timeout

    var errorDescription: String? {
        switch self {
        case .invalidResponse:
            return "Invalid server response."
        case let .http(status, body):
            return "HTTP \(status): \(body)"
        case .missingJobId:
            return "Submit response did not include a job ID."
        case .missingTranscript:
            return "Completed response did not include transcript text."
        case .remoteFailed:
            return "Transcription failed remotely."
        case .timeout:
            return "Transcription timed out."
        }
    }
}
