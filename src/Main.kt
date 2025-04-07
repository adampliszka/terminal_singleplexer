import java.awt.*
import java.awt.event.ActionEvent
import java.awt.event.ActionListener
import java.awt.event.KeyEvent
import javax.swing.*
import javax.swing.text.DefaultStyledDocument
import javax.swing.text.StyleConstants

object UIParams {
    const val TITLE = "Terminal singleplexer"
    const val PROMPT = "> "
    const val COLUMNS = 120;
    const val ROWS = 30;

    const val LINE_HEIGHT = 1.2f
    const val H_GAP = 0
    const val V_GAP = 0
    val LAYOUT = BorderLayout(H_GAP, V_GAP)

    val FONT: Font = Font("Monospaced", Font.PLAIN, 14)

    val BACKGROUND_PRIMARY: Color = Color.BLACK
    val BACKGROUND_SECONDARY: Color = Color.DARK_GRAY
    val PROMPT_COLOR: Color = Color.LIGHT_GRAY
    val TEXT_COLOR: Color = Color.WHITE
    val ERROR_COLOR: Color = Color.RED
    val SUCCESS_COLOR: Color = Color.GREEN
    val STDERR: Color = Color.PINK
}

class PromptInput(columns: Int) : JTextField(UIParams.COLUMNS) {
    var prompt = UIParams.PROMPT

    override fun setText(t: String) {
        super.setText(prompt + t)
    }

    override fun getText(): String {
        return super.getText().substring(prompt.length)
    }

    override fun processKeyEvent(e: KeyEvent) {
        if (selectionStart < prompt.length) {
            selectionStart = prompt.length
        }
        if ( e.id == KeyEvent.KEY_PRESSED && caretPosition == prompt.length ){
            if( e.keyCode == KeyEvent.VK_BACK_SPACE || e.keyCode == KeyEvent.VK_DELETE ) {
                e.consume()
                return
            }
            if (e.keyCode == KeyEvent.VK_LEFT || e.keyCode == KeyEvent.VK_HOME) {
                caretPosition = prompt.length
            }
        }
        super.processKeyEvent(e)
    }

    override fun cut() {
        if (selectionStart < prompt.length) {
            selectionStart = prompt.length
        }
        super.cut()
    }

    override fun replaceSelection(content: String?) {
        if (selectionStart < prompt.length) {
            selectionStart = prompt.length
        }
        super.replaceSelection(content)
    }
}

class TerminalGUI : JFrame() {
    private val outputField: JTextPane = JTextPane().apply {
        caretColor = UIParams.TEXT_COLOR
        font = UIParams.FONT
        isEditable = false
        document = DefaultStyledDocument()
        background = UIParams.BACKGROUND_SECONDARY
        foreground = UIParams.TEXT_COLOR
    }
    private val inputField: PromptInput = PromptInput(UIParams.COLUMNS).apply {
        caretColor = UIParams.TEXT_COLOR
        font = UIParams.FONT
        isEditable = true
        text = ""
        background = UIParams.BACKGROUND_SECONDARY
        foreground = UIParams.TEXT_COLOR
    }
//    private val location = ""
    // not going to implement browsing filesystem tbh - out of scope

    init {
        createUI()
    }

    private fun createUI() {
        defaultCloseOperation = DO_NOTHING_ON_CLOSE
        title = UIParams.TITLE
        layout = UIParams.LAYOUT
        background = UIParams.BACKGROUND_PRIMARY
        inputField.addActionListener(ExecuteListener())
        add(JScrollPane(outputField), BorderLayout.CENTER)
        add(inputField, BorderLayout.SOUTH)

        val fontMetrics = outputField.getFontMetrics(outputField.font)
        val rowHeight = fontMetrics.height
        val columnWidth = fontMetrics.charWidth('M')
        val preferredHeight = (rowHeight * UIParams.ROWS * UIParams.LINE_HEIGHT).toInt()
        val preferredWidth = columnWidth * UIParams.COLUMNS
        outputField.preferredSize = Dimension(preferredWidth, preferredHeight)
        inputField.preferredSize = Dimension(preferredWidth, (rowHeight * UIParams.LINE_HEIGHT).toInt())
        pack()
        setLocationRelativeTo(null)

        addWindowListener(object : java.awt.event.WindowAdapter() {
            override fun windowClosing(e: java.awt.event.WindowEvent) {
                val response = JOptionPane.showConfirmDialog(
                    this@TerminalGUI,
                    "Are you sure you want to exit?",
                    "Exit?",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.QUESTION_MESSAGE
                )
                if (response == JOptionPane.YES_OPTION) {
                    dispose()
                }
            }
        })
    }


    private inner class ExecuteListener : ActionListener {
        override fun actionPerformed(e: ActionEvent) {
            val command = inputField.text
            executeCommand(command)
        }
    }

    private fun executeCommand(command: String) {
        val p = inputField.prompt
        appendToOutput("$p$command\n", UIParams.PROMPT_COLOR)

        val processBuilder = ProcessBuilder(command.split(" "))
        processBuilder.redirectErrorStream(false)

        try {
            val process = processBuilder.start()

            val output = StringBuilder()
            val errorOutput = StringBuilder()
            val outputReader = process.inputStream.bufferedReader()
            val errorReader = process.errorStream.bufferedReader()
            var line: String?

            while (outputReader.readLine().also { line = it } != null) {
                output.append(line).append("\n")
            }

            while (errorReader.readLine().also { line = it } != null) {
                errorOutput.append(line).append("\n")
            }
            val exitCode = process.waitFor()

            appendToOutput(output.toString(), UIParams.TEXT_COLOR)
            appendToOutput(errorOutput.toString(), UIParams.STDERR)
            appendToOutput("Exit code: $exitCode\n", if (exitCode == 0) UIParams.SUCCESS_COLOR else UIParams.ERROR_COLOR)
        } catch (e: Exception) {
            appendToOutput("Error: ${e.message}", UIParams.ERROR_COLOR)
        }
        inputField.text = ""

    }

    private fun appendToOutput(text: String, color: Color) {
        val doc = outputField.styledDocument
        val style = outputField.addStyle("Style", null)
        StyleConstants.setForeground(style, color)
        doc.insertString(doc.length, text, style)
    }
}

fun main() {
    EventQueue.invokeLater {
        val ex = TerminalGUI()
        ex.isVisible = true
    }
}