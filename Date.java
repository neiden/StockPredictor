
public class Date {
    private String date;
    private double openPrice, closePrice, highPrice, lowPrice, nextOpenPrice, splitType;

    public Date(String date, double closePrice, double nextOpenPrice, double splitType){
        this.date = date;
        this.closePrice = closePrice;
        this.nextOpenPrice = nextOpenPrice;
        this.splitType = splitType;
    }

    public Date(String date, double openPrice, double highPrice, double lowPrice, double closePrice){
        this.date = date;
        this.openPrice = openPrice;
        this.closePrice = closePrice;
        this.highPrice = highPrice;
        this.lowPrice = lowPrice;
        nextOpenPrice = 0;
    }

    public String getDate(){
        return date;
    }

    public double getOpenPrice(){
        return openPrice;
    }

    public double getClosePrice(){
        return closePrice;
    }

    public double getLowPrice() {
        return lowPrice;
    }

    public double getHighPrice() {
        return highPrice;
    }


    public String toString(){
        return "Date: " + date + "\tOpen Price: " + openPrice + "\tHigh Price: " + highPrice + "\tLow Price: " + lowPrice + "\tClose Price: " + closePrice;
    }

    public double getNextOpenPrice() {
        return nextOpenPrice;
    }

    public void setNextOpenPrice(double nextOpenPrice) {
        this.nextOpenPrice = nextOpenPrice;
    }

    public double getSplitType() {
        return splitType;
    }

    public void setSplitType(double splitType) {
        this.splitType = splitType;
    }

    public void divideBy(double divisor){
        openPrice /= divisor;
        highPrice /= divisor;
        lowPrice /= divisor;
        closePrice /= divisor;
    }

}
