# LlamaCPP Completions R&D Plugin for JetBrains IDEs

An R&D code completion plugin for JetBrains IDEs that uses a local LLMs through Llama.cpp to provide context-aware code suggestions.

## Features

- 🚀 Local LLM-powered code completions
- 🔄 Real-time auto-suggestions
- 🎯 Context-aware completions using file content
- ⚡ Fast response times with local Llama.CPP server
- 🎨 Customizable completion behavior
- 📊 Performance monitoring
- 💾 Caching system for improved performance
- 🔧 Support for any GGUF model

## Prerequisites

- Java 17 or higher
- Docker for running the llama.cpp server
- IntelliJ IDEA 2023.3 or later
- At least 16GB RAM recommended
- A GGUF model file (e.g., Qwen, CodeLlama, or other compatible models)

## Demo

https://github.com/user-attachments/assets/2d336dfd-4c27-4fd0-82f2-07cb55702f59

## Installation

### Setting up the Model Server

1. Create a `models` directory in your project root:
   ```bash
   mkdir models
   ```

2. Download a GGUF model file and place it in the `models` directory. Recommended models:
    - [Qwen 1.5B Coder](https://huggingface.co/ggml-org/Qwen2.5-Coder-1.5B-Q8_0-GGUF/tree/main) 
    - Any other GGUF format model

   ```bash
   cd models
   wget https://huggingface.co/ggml-org/Qwen2.5-Coder-1.5B-Q8_0-GGUF/resolve/main/qwen2.5-coder-1.5b-q8_0.gguf?download=true  
   ```

3. Start the llama.cpp server using Docker:
   ```bash
   docker compose up --build
   ```

### Installing the Plugin

#### From Source
1. Clone the repository:
   ```bash
   git clone https://github.com/your-username/LlamaCPPCompletion.git
   cd LlamaCPPCompletion
   ```

2. Build the plugin:
   ```bash
   ./gradlew buildPlugin
   ```

3. Install in IntelliJ:
    - Go to Settings/Preferences → Plugins → ⚙️ → Install Plugin from Disk
    - Select the built plugin file from `build/distributions/`

#### From JetBrains Marketplace
- Open IntelliJ IDEA
- Go to Settings/Preferences → Plugins
- Search for "LlamaCPP Completions"
- Click Install

## Configuration

1. Open IntelliJ Settings/Preferences
3. Open the "LlamaCPP Completions" panel in sidebar
    - Server endpoint (default: http://127.0.0.1:8012)
    - Auto-trigger behavior
    - Maximum tokens
    - Response timeouts
    - Context window size

## Usage

### Basic Usage
1. Start typing in any editor
2. The plugin panel will show code completions

### Tool Window
- View all available completions in the dedicated tool window
- Double-click suggestions to insert them
- Use refresh button to manually update suggestions

## Contributing

We welcome contributions! Here's how you can help:

### Setting up Development Environment

1. Fork and clone the repository
2. Import as Gradle project in IntelliJ IDEA
3. Install required plugins:
    - Gradle
    - Plugin DevKit
    - Java

### Development Workflow

1. Create a feature branch:
   ```bash
   git checkout -b feature/your-feature-name
   ```

2. Make your changes following our coding conventions:
    - Use Java 17 features where appropriate
    - Follow IntelliJ Platform SDK guidelines
    - Add tests for new functionality
    - Update documentation as needed

3. Run tests:
   ```bash
   ./gradlew test
   ```

4. Build and verify:
   ```bash
   ./gradlew buildPlugin
   ```

5. Submit a Pull Request with:
    - Clear description of changes
    - Any related issue numbers
    - Screenshots for UI changes
    - Updated documentation

### Code Structure

```
LlamaCPPCompletion/
├── src/main/
│   ├── java/com/devoxx/llamacpp/
│   │   ├── actions/       # Action handlers
│   │   ├── completion/    # Completion logic
│   │   ├── core/         # Core functionality
│   │   ├── settings/     # Plugin settings
│   │   └── ui/           # User interface
│   └── resources/        # Icons and configs
```

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## Acknowledgments

- Thanks to the amazing [llama.cpp](https://github.com/ggerganov/llama.cpp) project ! 

## Support

- Report issues on GitHub

---

Made with ❤️ by Stephan Janssen
