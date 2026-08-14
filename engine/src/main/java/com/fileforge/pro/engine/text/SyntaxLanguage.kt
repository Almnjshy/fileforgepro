package com.fileforge.pro.engine.text

import com.fileforge.pro.core.filesystem.FileTypeDetector
import com.fileforge.pro.domain.model.FFile

/**
 * Programming language detection (Master Spec §44).
 *
 * Separate from [TextEditorEngine] so language detection can be reused
 * without loading file content.
 */
enum class SyntaxLanguage(
    val display: String,
    val extensions: Set<String>,
    val extensionlessNames: Set<String> = emptySet(),
) {
    KOTLIN("Kotlin", setOf("kt", "kts")),
    JAVA("Java", setOf("java")),
    PYTHON("Python", setOf("py", "pyw")),
    JAVASCRIPT("JavaScript", setOf("js", "jsx", "mjs", "cjs")),
    TYPESCRIPT("TypeScript", setOf("ts", "tsx")),
    JSON("JSON", setOf("json", "json5")),
    XML("XML", setOf("xml", "svg", "plist", "resx")),
    HTML("HTML", setOf("html", "htm", "xhtml")),
    CSS("CSS", setOf("css", "scss", "sass", "less")),
    SHELL("Shell", setOf("sh", "bash", "zsh"), setOf("bashrc", "zshrc")),
    SQL("SQL", setOf("sql")),
    YAML("YAML", setOf("yaml", "yml")),
    TOML("TOML", setOf("toml")),
    PROPERTIES("Properties", setOf("properties", "ini", "cfg", "conf")),
    GROOVY("Groovy", setOf("gradle", "groovy")),
    MARKDOWN("Markdown", setOf("md", "markdown")),
    PHP("PHP", setOf("php")),
    GO("Go", setOf("go")),
    RUST("Rust", setOf("rs")),
    C("C", setOf("c", "h")),
    CPP("C++", setOf("cpp", "hpp", "cc", "cxx")),
    CSHARP("C#", setOf("cs")),
    SWIFT("Swift", setOf("swift")),
    DART("Dart", setOf("dart")),
    RUBY("Ruby", setOf("rb"), setOf("gemfile", "rakefile")),
    DOCKERFILE("Dockerfile", emptySet(), setOf("dockerfile")),
    MAKEFILE("Makefile", emptySet(), setOf("makefile")),
    GITIGNORE("Git Ignore", emptySet(), setOf("gitignore")),
    EDITORCONFIG("EditorConfig", emptySet(), setOf("editorconfig")),
    ENV("Env", emptySet(), setOf("env", "env.local", "env.production")),
    PLAIN_TEXT("Plain Text", emptySet());

    companion object {
        fun fromExtension(ext: String?): SyntaxLanguage {
            if (ext == null) return PLAIN_TEXT
            val lower = ext.lowercase().trimStart('.')
            entries.firstOrNull { lower in it.extensions }?.let { return it }
            entries.firstOrNull { lower in it.extensionlessNames }?.let { return it }
            return PLAIN_TEXT
        }

        fun fromFileName(name: String): SyntaxLanguage {
            val lower = name.lowercase()
            entries.firstOrNull { lower in it.extensionlessNames }?.let { return it }
            val dot = lower.lastIndexOf('.')
            return if (dot > 0) fromExtension(lower.substring(dot + 1)) else PLAIN_TEXT
        }

        fun fromFile(file: FFile): SyntaxLanguage {
            if (file.isDirectory) return PLAIN_TEXT
            return fromFileName(file.name)
        }
    }
}
