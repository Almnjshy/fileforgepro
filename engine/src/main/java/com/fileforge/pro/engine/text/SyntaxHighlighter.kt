package com.fileforge.pro.engine.text

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import java.util.regex.Pattern

/**
 * Token type for syntax highlighting.
 */
enum class TokenKind {
    KEYWORD, STRING, COMMENT, NUMBER, OPERATOR, IDENTIFIER,
    ANNOTATION, TYPE, FUNCTION, PUNCTUATION, PLAIN,
}

/**
 * Syntax highlighter (Master Spec §44).
 *
 * Uses regex-based tokenization per language. Not a full parser — good enough
 * for editor preview. Returns an [AnnotatedString] that Compose can render
 * directly in a Text composable.
 */
object SyntaxHighlighter {

    // Color palette (dark theme-friendly)
    private val COLOR_KEYWORD = Color(0xFFC792EA)     // purple
    private val COLOR_STRING = Color(0xFFC3E88D)      // green
    private val COLOR_COMMENT = Color(0xFF546E7A)     // gray
    private val COLOR_NUMBER = Color(0xFFF78C6C)      // orange
    private val COLOR_OPERATOR = Color(0xFF89DDFF)    // cyan
    private val COLOR_TYPE = Color(0xFFFFCB6B)        // yellow
    private val COLOR_FUNCTION = Color(0xFF82AAFF)    // blue
    private val COLOR_ANNOTATION = Color(0xFFFFCB6B)  // yellow
    private val COLOR_PLAIN = Color(0xFFEEFFFF)

    fun highlight(text: String, language: SyntaxLanguage): AnnotatedString {
        return androidx.compose.ui.text.buildAnnotatedString {
            if (text.isEmpty()) return@buildAnnotatedString
            when (language) {
                SyntaxLanguage.KOTLIN, SyntaxLanguage.GROOVY ->
                    highlightWithPatterns(text, this, LanguagePatterns.KOTLIN)
                SyntaxLanguage.JAVA ->
                    highlightWithPatterns(text, this, LanguagePatterns.JAVA)
                SyntaxLanguage.PYTHON ->
                    highlightWithPatterns(text, this, LanguagePatterns.PYTHON)
                SyntaxLanguage.JAVASCRIPT, SyntaxLanguage.TYPESCRIPT ->
                    highlightWithPatterns(text, this, LanguagePatterns.JS)
                SyntaxLanguage.JSON ->
                    highlightWithPatterns(text, this, LanguagePatterns.JSON)
                SyntaxLanguage.XML, SyntaxLanguage.HTML ->
                    highlightWithPatterns(text, this, LanguagePatterns.XML)
                SyntaxLanguage.CSS ->
                    highlightWithPatterns(text, this, LanguagePatterns.CSS)
                SyntaxLanguage.SHELL ->
                    highlightWithPatterns(text, this, LanguagePatterns.SHELL)
                SyntaxLanguage.SQL ->
                    highlightWithPatterns(text, this, LanguagePatterns.SQL)
                else -> append(text) // plain
            }
        }
    }

    private fun highlightWithPatterns(
        text: String,
        builder: AnnotatedString.Builder,
        patterns: LanguagePatterns,
    ) {
        // Simple approach: tokenize via combined regex
        val combined = patterns.combinedPattern
        val matcher = combined.matcher(text)
        var lastEnd = 0

        while (matcher.find()) {
            if (matcher.start() > lastEnd) {
                builder.append(text.substring(lastEnd, matcher.start()))
            }
            val tokenText = matcher.group()
            val kind = patterns.classify(matcher)
            val color = colorFor(kind)
            builder.withStyle(SpanStyle(color = color, fontFamily = FontFamily.Monospace)) {
                append(tokenText)
            }
            lastEnd = matcher.end()
        }
        if (lastEnd < text.length) {
            builder.append(text.substring(lastEnd))
        }
    }

    private fun colorFor(kind: TokenKind): Color = when (kind) {
        TokenKind.KEYWORD -> COLOR_KEYWORD
        TokenKind.STRING -> COLOR_STRING
        TokenKind.COMMENT -> COLOR_COMMENT
        TokenKind.NUMBER -> COLOR_NUMBER
        TokenKind.OPERATOR -> COLOR_OPERATOR
        TokenKind.TYPE -> COLOR_TYPE
        TokenKind.FUNCTION -> COLOR_FUNCTION
        TokenKind.ANNOTATION -> COLOR_ANNOTATION
        TokenKind.PUNCTUATION, TokenKind.IDENTIFIER, TokenKind.PLAIN -> COLOR_PLAIN
    }
}

/**
 * Per-language regex patterns for tokenization.
 */
private class LanguagePatterns(
    val keywordPattern: String,
    val commentLinePattern: String,
    val commentBlockPattern: String,
    val stringPattern: String,
    val numberPattern: String,
    val annotationPattern: String,
) {
    val combinedPattern: Pattern by lazy {
        val parts = listOf(
            commentBlockPattern,
            commentLinePattern,
            stringPattern,
            annotationPattern,
            keywordPattern,
            numberPattern,
            "\\b[A-Z][A-Za-z0-9_]*\\b", // Type (Capitalized identifier)
            "\\b[a-z_][A-Za-z0-9_]*(?=\\s*\\()", // function call
            "[+\\-*/%=<>!&|^~?:]+", // operators
            "[{}()\\[\\];,.]", // punctuation
            "\\S+", // any other
        )
        Pattern.compile(parts.joinToString("|"))
    }

    fun classify(m: java.util.regex.MatchResult): TokenKind {
        val text = m.group()
        return when {
            commentBlockPattern.toRegex().matches(text) -> TokenKind.COMMENT
            commentLinePattern.toRegex().matches(text) -> TokenKind.COMMENT
            stringPattern.toRegex().matches(text) -> TokenKind.STRING
            annotationPattern.toRegex().matches(text) -> TokenKind.ANNOTATION
            keywordPattern.toRegex().matches(text) -> TokenKind.KEYWORD
            numberPattern.toRegex().matches(text) -> TokenKind.NUMBER
            text.matches(Regex("[+\\-*/%=<>!&|^~?:]+")) -> TokenKind.OPERATOR
            text.matches(Regex("[{}()\\[\\];,.]")) -> TokenKind.PUNCTUATION
            text[0].isUpperCase() -> TokenKind.TYPE
            else -> TokenKind.IDENTIFIER
        }
    }

    companion object {
        val KOTLIN = LanguagePatterns(
            keywordPattern = "\\b(fun|val|var|class|interface|object|enum|sealed|data|companion|override|private|public|protected|internal|abstract|open|final|static|import|package|return|if|else|when|for|while|do|break|continue|in|is|as|throw|try|catch|finally|null|true|false|this|super|by|lateinit|init|constructor|suspend|inline|operator|infix|tailrec|crossinline|noinline|reified|where|out|in|vararg|const|external|infix)\\b",
            commentLinePattern = "//[^\\n]*",
            commentBlockPattern = "/\\*[\\s\\S]*?\\*/",
            stringPattern = "\"(?:\\\\.|[^\"\\\\])*\"|'(?:\\\\.|[^'\\\\])*'|\"\"\"[\\s\\S]*?\"\"\"",
            numberPattern = "\\b\\d[\\d_]*(?:\\.\\d+)?[fFlLdD]?\\b|0x[0-9A-Fa-f_]+|0b[01_]+",
            annotationPattern = "@[A-Za-z_][A-Za-z0-9_.]*",
        )

        val JAVA = LanguagePatterns(
            keywordPattern = "\\b(public|private|protected|static|final|class|interface|enum|extends|implements|import|package|new|return|if|else|for|while|do|break|continue|switch|case|default|try|catch|finally|throw|throws|null|true|false|this|super|void|int|long|double|float|boolean|char|byte|short|String|var|instanceof|synchronized|volatile|transient|native|abstract|strictfp)\\b",
            commentLinePattern = "//[^\\n]*",
            commentBlockPattern = "/\\*[\\s\\S]*?\\*/",
            stringPattern = "\"(?:\\\\.|[^\"\\\\])*\"|'(?:\\\\.|[^'\\\\])*'",
            numberPattern = "\\b\\d[\\d_]*(?:\\.\\d+)?[fFlLdD]?\\b|0x[0-9A-Fa-f_]+|0b[01_]+",
            annotationPattern = "@[A-Za-z_][A-Za-z0-9_.]*",
        )

        val PYTHON = LanguagePatterns(
            keywordPattern = "\\b(def|class|import|from|as|return|if|elif|else|for|while|break|continue|in|is|not|and|or|try|except|finally|raise|with|lambda|yield|global|nonlocal|pass|del|assert|None|True|False|self|async|await)\\b",
            commentLinePattern = "#[^\\n]*",
            commentBlockPattern = "\"\"\"[\\s\\S]*?\"\"\"",
            stringPattern = "\"(?:\\\\.|[^\"\\\\])*\"|'(?:\\\\.|[^'\\\\])*'|\"\"\"[\\s\\S]*?\"\"\"",
            numberPattern = "\\b\\d+(?:\\.\\d+)?[jJ]?\\b|0x[0-9A-Fa-f_]+|0b[01_]+|0o[0-7_]+",
            annotationPattern = "@[A-Za-z_][A-Za-z0-9_.]*",
        )

        val JS = LanguagePatterns(
            keywordPattern = "\\b(function|var|let|const|class|extends|implements|interface|type|enum|import|export|from|as|default|new|return|if|else|for|while|do|break|continue|switch|case|try|catch|finally|throw|typeof|instanceof|in|of|null|undefined|true|false|this|super|void|delete|async|await|yield|static|get|set|public|private|protected|readonly|abstract|namespace|declare|module)\\b",
            commentLinePattern = "//[^\\n]*",
            commentBlockPattern = "/\\*[\\s\\S]*?\\*/",
            stringPattern = "\"(?:\\\\.|[^\"\\\\])*\"|'(?:\\\\.|[^'\\\\])*'|`(?:\\\\.|[^`\\\\])*`",
            numberPattern = "\\b\\d+(?:\\.\\d+)?[eE]?\\d*\\b|0x[0-9A-Fa-f_]+|0b[01_]+|0o[0-7_]+",
            annotationPattern = "@[A-Za-z_][A-Za-z0-9_.]*",
        )

        val JSON = LanguagePatterns(
            keywordPattern = "\\b(true|false|null)\\b",
            commentLinePattern = "",
            commentBlockPattern = "",
            stringPattern = "\"(?:\\\\.|[^\"\\\\])*\"",
            numberPattern = "-?\\b\\d+(?:\\.\\d+)?(?:[eE][+-]?\\d+)?\\b",
            annotationPattern = "",
        )

        val XML = LanguagePatterns(
            keywordPattern = "</?[A-Za-z_][A-Za-z0-9_:.\\-]*|/?>",
            commentLinePattern = "",
            commentBlockPattern = "<!--[\\s\\S]*?-->",
            stringPattern = "\"[^\"]*\"|'[^']*'",
            numberPattern = "",
            annotationPattern = "",
        )

        val CSS = LanguagePatterns(
            keywordPattern = "\\b(color|background|background-color|margin|padding|border|width|height|display|position|top|left|right|bottom|font|font-size|font-weight|font-family|text-align|line-height|flex|grid|gap|opacity|z-index)\\b",
            commentLinePattern = "//[^\\n]*",
            commentBlockPattern = "/\\*[\\s\\S]*?\\*/",
            stringPattern = "\"[^\"]*\"|'[^']*'",
            numberPattern = "-?\\b\\d+(?:\\.\\d+)?(?:px|em|rem|%|vh|vw|pt|pc|in|cm|mm|s|ms)?\\b",
            annotationPattern = "",
        )

        val SHELL = LanguagePatterns(
            keywordPattern = "\\b(if|then|else|elif|fi|for|in|do|done|while|case|esac|function|return|echo|printf|read|set|unset|export|local|declare|alias|source|cd|pwd|ls|grep|sed|awk|find|xargs)\\b",
            commentLinePattern = "#[^\\n]*",
            commentBlockPattern = "",
            stringPattern = "\"(?:\\\\.|[^\"\\\\])*\"|'(?:\\\\.|[^'\\\\])*'",
            numberPattern = "\\b\\d+\\b",
            annotationPattern = "\\$[A-Za-z_][A-Za-z0-9_]*|\\$\\{[^}]+\\}",
        )

        val SQL = LanguagePatterns(
            keywordPattern = "\\b(SELECT|FROM|WHERE|INSERT|INTO|VALUES|UPDATE|SET|DELETE|CREATE|TABLE|ALTER|DROP|INDEX|VIEW|JOIN|LEFT|RIGHT|INNER|OUTER|ON|GROUP|BY|HAVING|ORDER|ASC|DESC|LIMIT|OFFSET|DISTINCT|UNION|ALL|AS|AND|OR|NOT|NULL|IS|IN|LIKE|BETWEEN|EXISTS|CASE|WHEN|THEN|ELSE|END|COUNT|SUM|AVG|MIN|MAX|PRIMARY|KEY|FOREIGN|REFERENCES|DEFAULT|UNIQUE|CONSTRAINT|CHECK|BEGIN|COMMIT|ROLLBACK|TRANSACTION)\\b",
            commentLinePattern = "--[^\\n]*",
            commentBlockPattern = "/\\*[\\s\\S]*?\\*/",
            stringPattern = "'(?:''|[^'])*'",
            numberPattern = "\\b\\d+(?:\\.\\d+)?\\b",
            annotationPattern = "",
        )
    }
}
