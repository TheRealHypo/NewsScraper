import com.gargoylesoftware.htmlunit.FailingHttpStatusCodeException
import com.gargoylesoftware.htmlunit.WebClient
import com.gargoylesoftware.htmlunit.html.HtmlPage

/**
 * @author Dominik Elflein
 */
abstract class ScraperJob implements Runnable{

    String scraperName
    String id

    List<Result> results = []

    Log logger = NewsScraperMain.logger
    DatabaseController db = NewsScraperMain.db

    boolean enabled = true
    boolean stopped = true

    /**
     * Superclass for every Scraper.
     * -> Gets ID from Database
     * -> Disables if no ID was found.
     *
     * @param name - Name of the Scraper
     */
    ScraperJob(String name){
        this.scraperName = name
        this.id = db.getScraperID(name)
        if(id == null){
            logger.warn("Failed to initialize Scraper $scraperName, no ID found.")
            disable()
        }else{
            logger.info("Initialized Scraper $scraperName with ID: $id")
            enable()
        }
    }

    /**
     * Get Basic Page Source without execution of CSS or JavaScript.
     * If JavaScript is needed, or dynamic Content has to grabbed, use getPageResponseWithJS()
     *
     * @param url
     * @return HtmlPage, or null if Error occurs.
     */
    HtmlPage getPageResponse(String url){
        try{
            final WebClient webClient = new WebClient()
            webClient.getOptions().setJavaScriptEnabled(false)
            webClient.getOptions().setCssEnabled(false)
            webClient.getOptions().setAppletEnabled(false)
            return webClient.getPage(url)
        }catch(UnknownHostException e){
            e.printStackTrace()
            logger.error("Couldn't connect to $url, UnknownHostException!")
            return null
        }catch(FailingHttpStatusCodeException e1){
            e1.printStackTrace()
            logger.error("Page responded with bad HTTP-Code")
            return null
        }
    }

    /**
     * Overloaded Function, to also use URL objects if needed.
     *
     * @param url
     * @return HtmlPage or Null if Error occurs.
     */
    HtmlPage getPageResponse(URL url){
        getPageResponse(url.getPath())
    }

    /**
     * If Site contains dynamically loaded Conent ot JavaScript is needed, this will
     * return the HtmlPage that is needed to crawl a Website.
     *
     * If any JavaScript Errors appear in console, please use getPageResponse() instead.
     * If Not everything is loaded, try to increase the waiting Time.
     *
     * @param url
     * @param wait - Maximum Time to wait for Javascript to finish (default: 5000ms)
     * @return HtmlPage or Null if Error occurs.
     */
    HtmlPage getPageResponseWithJS(String url, long wait = 5000){
        try{
            final WebClient webClient = new WebClient()
            int missingScripts = webClient.waitForBackgroundJavaScript(wait)
            if(missingScripts > 0){
                logger.warn("There are $missingScripts scripts left for Execution.")
            }
            return webClient.getPage(url)
        }catch(UnknownHostException e){
            e.printStackTrace()
            logger.error("Couldn't connect to $url, UnknownHostException!")
        }catch(FailingHttpStatusCodeException e1){
            logger.error("Page responded with bad HTTP-Code")
            return null
        }
    }

    /**
     * Overloaded Function, to also use URL objects if needed.
     *
     * @param url
     * @return HtmlPage or Null if Error occurs.
     */
    HtmlPage getPageResponseWithJS(URL url, long wait = 5000){
        getPageResponseWithJS(url.getPath(), wait)
    }

    /**
     * Adds a Result to the Result List that is in Memory.
     *
     * If more than 50 results are cached, this will call saveResults().
     *
     */
    private def addResult(String key, String obj, Date date, String source){
        results << new Result(key, obj, date, source)
        //logger.debug("Added new Object(${obj.length() > 60 ? obj.substring(0, 60).replaceAll("\n", "") : obj.replaceAll("\n", "")}) to Resultlist of $scraperName")
        if(results.size() > 50){
            saveResults()
            logger.info("Saved 50 Results.")
        }
    }

    /**
     * Adds Text to the Result List
     *
     * @param text -> Text to be saved
     * @param date -> Date of Source
     * @param source -> SourceURL
     */
    def addText(String text, Date date, String source){
        addResult("TEXT", text, date, source)
    }

    /**
     * Adds ImageLing to the Result List
     *
     * @param webImageLink -> Link to be saved
     * @param date -> Date of Source
     * @param source -> SourceURL
     */
    def addWebImage(String webImageLink, Date date, String source){
        addResult("WEBIMAGE", webImageLink, date, source)
    }

    /**
     * Call this function to manually save all cached Results.
     * Clears the Cache after Saving finished.
     *
     * ex. Call this function before execution ends, to save leftovers.
     */
    def saveResults(){
        logger.info("Saving results ...")
        results.eachWithIndex { entry, int i ->
           String type = entry.type
           String obj = entry.obj
           Date date = entry.date
           String source = entry.source
           switch (type){
               case "TEXT":
                   db.addTextResult(id, obj as String, date, source)
                   break
               case "WEBIMAGE":
                   db.addWebImageResult(id, obj as String, date, source)
                   break
               default:
                   logger.warn("Trying to add $obj as Text to database. Type unknown.")
                   db.addTextResult(id, obj as String, date, source)
                   break
           }
       }
        results.clear()
    }

    /**
     * Call this function to Run the Scraper it will safe check if Scraper is enabled,
     * or is already running.
     *
     * Sets flag to safely stop the Scraper
     *
     * @return
     */
    def runScraper(){
        if(!enabled) {
            logger.warn("Scraper $scraperName is diabled!")
        }else {
            if (stopped) {
                stopped = false
                this.run()
            } else {
                logger.warn("$scraperName already running.")
            }
        }
    }

    /**
     * Enables the Scraper to be executed.
     *
     */
    def enable(){
        enabled = true
    }

    /**
     * Force disable the Scraper to prevent execution.
     *
     */
    def disable(){
        enabled = false
    }

    /**
     * @return true if scraper shall be stopped
     */
    boolean isStopped(){
        return stopped
    }

    /**
     * Private Class Result, for maintaining variables.
     */
    private class Result{
        Date date = null
        String obj
        String source

        String type

        Result(String type, String obj, Date date, String source){
            this.type = type
            this.obj = obj
            this.date = date
            this.source = source
        }
    }
}
