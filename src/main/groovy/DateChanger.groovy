import com.gargoylesoftware.htmlunit.FailingHttpStatusCodeException
import com.gargoylesoftware.htmlunit.WebClient
import com.gargoylesoftware.htmlunit.html.DomNode
import com.gargoylesoftware.htmlunit.html.HtmlPage
import com.gargoylesoftware.htmlunit.html.HtmlUnknownElement
import java.sql.Connection
import java.sql.DriverManager
import java.sql.ResultSet
import java.sql.Statement
import java.text.SimpleDateFormat

/**
 * 7549e976e49ee6cf9a49b16e39bf1951	SpiegelOnline
 41df27494f8a9c6a63ada94710d8445b	Die Welt
 a05700b56a646dbcc5966749b1d83f4e	Die Bild
 e24ee2487879116dcab772c0ac4fe341	Focus
 dc995614724baff087b9715538722a46	Süddeutsche
 */

final String CONNECTION_STRING = "jdbc:h2:C:\\Users\\Domin\\Documents\\LibGDX-Workspace\\NewsScraper\\build\\libs\\database\\ScraperData"
Connection conn = DriverManager.getConnection(CONNECTION_STRING)

Map<String, String> links = [:]

Statement stm = conn.createStatement()
ResultSet rs = stm.executeQuery("SELECT * FROM SCRAPERDATA.PUBLIC.SCRAPER_RESULTS WHERE SCRAPER_ID = '7549e976e49ee6cf9a49b16e39bf1951' AND HOUR(DATE) = 0")
while(rs.next()){
    links.put(rs.getString("ID"), rs.getString("SOURCE"))
}
stm.close()
conn.close()

links.each {
    HtmlPage page = getPageResponse(it.value)
    if (page != null) {

        String category = it.value.split("/")[3]

        DomNode time = page.querySelector(".timeformat")
        if(time){
            String dateString = time.getAttributes().getNamedItem("datetime").nodeValue

            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd k:mm:ss")
            Date date = sdf.parse(dateString)

            updateDateAndCategory(it.key, date, category)
        }else{
            updateDateAndCategory(it.key, null, category)
        }
    }
}

static HtmlPage getPageResponse(String url){
    try{
        final WebClient webClient = new WebClient()
        webClient.getOptions().setJavaScriptEnabled(false)
        webClient.getOptions().setCssEnabled(false)
        webClient.getOptions().setAppletEnabled(false)
        webClient.getOptions().setThrowExceptionOnScriptError(false)
        return webClient.getPage(url)
    }catch(UnknownHostException e){
        e.printStackTrace()
        return null
    }catch(FailingHttpStatusCodeException e1){
        e1.printStackTrace()
        return null
    }
}

static updateDateAndCategory(String id, Date date, String category){
    Connection conn = DriverManager.getConnection("jdbc:h2:C:\\Users\\Domin\\Documents\\LibGDX-Workspace\\NewsScraper\\build\\libs\\database\\ScraperData")
    Statement stm = conn.createStatement()

    String dateString = null
    if(date!=null){
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss")
        dateString = sdf.format(date)
    }

    println("Updating Row with ID:$id, to accept date: $dateString")
    if(dateString == null){
        stm.executeUpdate("UPDATE SCRAPERDATA.PUBLIC.SCRAPER_RESULTS SET DATE=NULL WHERE SCRAPERDATA.PUBLIC.SCRAPER_RESULTS.ID='$id'")
    }else{
        stm.executeUpdate("UPDATE SCRAPERDATA.PUBLIC.SCRAPER_RESULTS SET DATE='$dateString' WHERE SCRAPERDATA.PUBLIC.SCRAPER_RESULTS.ID='$id'")
    }
    stm.executeUpdate("UPDATE SCRAPERDATA.PUBLIC.SCRAPER_RESULTS SET CATEGORY='$category' WHERE SCRAPERDATA.PUBLIC.SCRAPER_RESULTS.ID='$id'")
    stm.close()
    conn.close()
}


