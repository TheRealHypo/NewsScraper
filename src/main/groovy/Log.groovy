import javafx.application.Platform
import javafx.scene.control.TextArea
import java.text.SimpleDateFormat

/**
 * @author Dominik Elflein
 */
class Log {

    private TextArea log = NewsScraperMain.log

    private enum LoggingLevel {
        INFO,
        WARN,
        ERROR,
        DEBUG
    }

    /**
     * Defines the current Logging Level.
     *
     * DEBUG -> All information visible
     * INFO -> Debug information isn't shown
     * WARN -> Only Warns and Errors are shown.
     * ERROR -> Only Errors are shown.
     */
    private LoggingLevel lvl = LoggingLevel.DEBUG

    /**
     * Prints Debug Message to log.
     *
     * @param message
     */
    def debug(String message){
        if(lvl == LoggingLevel.DEBUG){
            message = message.replaceAll("\n", "")
            Platform.runLater {
                log.appendText("${getTime()} - Debug: $message\n")
            }
        }
    }

    /**
     * Prints Information to log.
     *
     * @param message
     */
    def info(String message){
        if(lvl == LoggingLevel.INFO || lvl == LoggingLevel.DEBUG){
            message = message.replaceAll("\n", "")
            Platform.runLater {
                log.appendText("${getTime()} - Info: $message\n")
            }
        }
    }

    /**
     * Prints Warning Message to log.
     *
     * @param message
     */
    def warn(String message){
        if(lvl == LoggingLevel.WARN || lvl == LoggingLevel.INFO || lvl == LoggingLevel.DEBUG ){
            message = message.replaceAll("\n", "")
            Platform.runLater {
                log.appendText("${getTime()} - Warning: $message\n")
            }
        }
    }

    /**
     * Prints Error Message to log.
     *
     * @param message
     */
    def error(String message){
        if(lvl == LoggingLevel.ERROR || lvl == LoggingLevel.WARN || lvl == LoggingLevel.INFO || lvl == LoggingLevel.DEBUG ){
            message = message.replaceAll("\n", "")
            Platform.runLater {
                log.appendText("${getTime()} - ERROR: $message\n")
            }
        }
    }

    /**
     * Gets a Timestamp for the Current System time.
     *
     * @return timestamp hh:mm:ss
     */
    static String getTime(){
        Date date = new Date(System.currentTimeMillis())
        SimpleDateFormat sdf = new SimpleDateFormat("k:mm:ss")
        sdf.format(date)
    }
}
