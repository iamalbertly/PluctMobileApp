import SwiftUI

struct ContentView: View {
    @StateObject private var viewModel = PluctViewModel()

    var body: some View {
        TabView {
            NavigationStack {
                HomeView(viewModel: viewModel)
            }
            .tabItem {
                Label("Home", systemImage: "link")
            }

            NavigationStack {
                ResultView(viewModel: viewModel)
            }
            .tabItem {
                Label("Result", systemImage: "doc.text")
            }

            NavigationStack {
                SettingsView(viewModel: viewModel)
            }
            .tabItem {
                Label("Settings", systemImage: "gearshape")
            }
        }
    }
}

private struct HomeView: View {
    @ObservedObject var viewModel: PluctViewModel

    var body: some View {
        List {
            Section {
                TextField("Paste TikTok URL", text: $viewModel.urlText, axis: .vertical)
                    .textInputAutocapitalization(.never)
                    .keyboardType(.URL)
                    .autocorrectionDisabled()

                HStack {
                    Button("Paste") {
                        viewModel.pasteFromClipboard()
                    }
                    Spacer()
                    Text(viewModel.walletLabel)
                        .foregroundStyle(.secondary)
                }

                Button("TikTok link -> Text") {
                    Task { await viewModel.startTranscription() }
                }
                .buttonStyle(.borderedProminent)
                .disabled(viewModel.isBusy)
            }

            if let validationMessage = viewModel.validationMessage {
                Section {
                    Text(validationMessage)
                        .foregroundStyle(viewModel.isCurrentUrlValid ? .secondary : .red)
                }
            }

            Section("Processing") {
                ProcessingRow(title: "Queued", isActive: viewModel.processingState == .queued)
                ProcessingRow(title: "Processing", isActive: viewModel.processingState == .processing)
                ProcessingRow(title: "Completed", isActive: viewModel.processingState == .completed)
                ProcessingRow(title: "Failed", isActive: viewModel.processingState == .failed)
            }
        }
        .navigationTitle("Pluct")
    }
}

private struct ProcessingRow: View {
    let title: String
    let isActive: Bool

    var body: some View {
        HStack {
            Image(systemName: isActive ? "checkmark.circle.fill" : "circle")
                .foregroundStyle(isActive ? .green : .secondary)
            Text(title)
        }
    }
}

private struct ResultView: View {
    @ObservedObject var viewModel: PluctViewModel

    var body: some View {
        List {
            Section("Transcript") {
                Text(viewModel.transcript.isEmpty ? "No transcript yet." : viewModel.transcript)
                    .textSelection(.enabled)
                    .frame(maxWidth: .infinity, alignment: .leading)
            }

            Section {
                Button("Copy") {
                    viewModel.copyTranscript()
                }
                .disabled(viewModel.transcript.isEmpty)

                Button("Retry") {
                    Task { await viewModel.retry() }
                }
                .disabled(viewModel.urlText.isEmpty || viewModel.isBusy)
            }
        }
        .navigationTitle("Result")
    }
}

private struct SettingsView: View {
    @ObservedObject var viewModel: PluctViewModel

    var body: some View {
        List {
            Section("API") {
                TextField("API environment", text: $viewModel.apiEnvironment)
                    .textInputAutocapitalization(.never)
                    .keyboardType(.URL)
                    .autocorrectionDisabled()
            }

            Section("Device") {
                LabeledContent("Device ID", value: viewModel.deviceId)
                LabeledContent("App version", value: viewModel.appVersion)
            }

            Section("Diagnostics") {
                Text(viewModel.diagnosticsText)
                    .font(.footnote.monospaced())
                    .textSelection(.enabled)
            }
        }
        .navigationTitle("Settings")
    }
}
