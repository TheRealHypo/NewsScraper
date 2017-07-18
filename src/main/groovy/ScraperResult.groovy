import java.sql.Timestamp
import java.text.SimpleDateFormat

/**
 * Created by Domin on 15.07.2017.
 */
class ScraperResult {

    String id
    String scraper_id
    String type
    String result
    Date date
    String source
    String category

    ScraperResult(String id, String scraper_id, String type, String result, Timestamp date, String source, String category) {
        this.id = id
        this.scraper_id = scraper_id
        this.type = type
        this.result = result
        this.date = date
        this.source = source
        this.category = category
    }

    String getDateAsString(){
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd k:mm")
        if(this.date != null){
            return sdf.format(this.date)
        }else{
            return null
        }
    }
}
