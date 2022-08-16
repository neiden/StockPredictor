import java.util.*;
import java.io.FileInputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class StockPredictor {

    static Connection conn = null;

    public static void main(String[] args) throws Exception {
        // Get connection properties
        String paramsFile = "ConnectionParameters.txt";
        if (args.length >= 1) {
            paramsFile = args[0];
        }
        Properties connectprops = new Properties();
        connectprops.load(new FileInputStream(paramsFile));

        try {
            // Get connection
            Class.forName("com.mysql.jdbc.Driver");
            String dburl = connectprops.getProperty("dburl");
            String username = connectprops.getProperty("user");
            conn = DriverManager.getConnection(dburl, connectprops);
            System.out.printf("Database connection %s %s established.%n", dburl, username);

            // Enter Ticker and TransDate, Fetch data for that ticker and date
            Scanner in = new Scanner(System.in);
            while (true) {
                System.out.print("Enter ticker and date (YYYY.MM.DD): ");
                String[] data = in.nextLine().trim().split("\\s+");
                if(data[0].equals("")){
                    System.out.println("Database Connection Terminated");
                    break;
                }
                String name = findName(data[0]);
                if(!(name.equals(""))) {
                    System.out.println(name);
                    if (data.length == 1) {
                        calculateStocks(data[0], "", "");
                    } else {
                        calculateStocks(data[0], data[1], data[2]);
                    }
                }
                else{
                    System.out.println("Ticker not found, try again.");
                }
            }

            conn.close();
        } catch (SQLException ex) {
            System.out.printf("SQLException: %s%nSQLState: %s%nVendorError: %s%n" ,
                    ex.getMessage(), ex.getSQLState(), ex.getErrorCode());
        }
    }
    static String findName(String ticker) throws SQLException{
        PreparedStatement ps = conn.prepareStatement("select Name from Company where Ticker = ?");
        ps.setString(1, ticker);
        ResultSet rs = ps.executeQuery();
        if(rs.next()){
            return rs.getString(1);
        }
        ps.close();
        return "";

    }

    static void calculateStocks(String ticker, String start, String end) throws SQLException {
        // Prepare query
        PreparedStatement pstmt;
        if(start.equals("") && end.equals("")) {

            pstmt = conn.prepareStatement(
                    "select TransDate, OpenPrice, HighPrice, LowPrice, ClosePrice " +
                            "  from PriceVolume " +
                            "  where Ticker = ? order by TransDate desc");
            // Fill in the blanks
            pstmt.setString(1, ticker);
        }
        else {
            pstmt = conn.prepareStatement(
                    "select TransDate, OpenPrice, HighPrice, LowPrice, ClosePrice " +
                            "  from PriceVolume " +
                            "  where Ticker = ? and TransDate between ? and ? order by TransDate desc");

            // Fill in the blanks
            pstmt.setString(1, ticker);
            pstmt.setString(2, start);
            pstmt.setString(3, end);
        }
        ResultSet rs = pstmt.executeQuery();
        ArrayList<Date> splitDays = new ArrayList<>();
        ArrayList<Date> tradingDays = new ArrayList<>();

        while (rs.next()) {
            tradingDays.add(new Date(rs.getString(1), rs.getDouble(2), rs.getDouble(3), rs.getDouble(4), rs.getDouble(5)));
        }

        splitAdjust(tradingDays, splitDays);

        for(int i = 0; i < splitDays.size(); i++){
            if(splitDays.get(i).getSplitType() == 2){
                System.out.println("2:1 split on " + splitDays.get(i).getDate() + " "
                        + splitDays.get(i).getClosePrice() + " --> " + splitDays.get(i).getNextOpenPrice());
            }
            if(splitDays.get(i).getSplitType() == 3){
                System.out.println("3:1 split on " + splitDays.get(i).getDate() + " "
                        + splitDays.get(i).getClosePrice() + " --> " + splitDays.get(i).getNextOpenPrice());
            }
            if(splitDays.get(i).getSplitType() == 1.5){
                System.out.println("3:2 split on " + splitDays.get(i).getDate() + " "
                        + splitDays.get(i).getClosePrice() + " --> " + splitDays.get(i).getNextOpenPrice());
            }
        }
        System.out.println(splitDays.size() + " splits in " + tradingDays.size() + " trading days\n");
        investingStrategy(tradingDays);

        pstmt.close();
    }

    static void investingStrategy(ArrayList<Date> tradingDays){
        double cash = 0;
        int shares = 0;
        int transactionNum = 0;
        double sum = 0;
        double closingAvg = 0;
        for(int i = tradingDays.size()-1; i >= 0 ; i--){
            if(tradingDays.size() - i < 50){
                sum += tradingDays.get(i).getClosePrice();
            }
            if(tradingDays.size() - i == 50){
                sum += tradingDays.get(i).getClosePrice();
                closingAvg = sum / 50.0;
            }
            if(tradingDays.size() - i > 50){

                //Buy
                if((tradingDays.get(i).getClosePrice() < closingAvg) && ((tradingDays.get(i).getClosePrice() / tradingDays.get(i).getOpenPrice()) < .97000001)){
                    shares += 100;
                    cash -= 100*(tradingDays.get(i-1).getOpenPrice()) + 8;
                    transactionNum++;
                }
                //Sell
                else if((shares >= 100) && (tradingDays.get(i).getOpenPrice() > closingAvg) &&
                        ((tradingDays.get(i).getOpenPrice() / tradingDays.get(i + 1).getClosePrice()) > 1.00999999)){
                    shares -= 100;
                    cash += 100*((tradingDays.get(i).getOpenPrice() + tradingDays.get(i).getClosePrice())/2) - 8;
                    transactionNum++;
                }
                //Sell remaining shares
                if(i == 0){
                    cash += shares*(tradingDays.get(i).getOpenPrice());
                }
                //Adjust 50 day average
                sum -= tradingDays.get(i + 50).getClosePrice();
                sum += tradingDays.get(i).getClosePrice();
                closingAvg = sum / 50.0;
            }
        }
        System.out.println("Executing investment strategy");
        System.out.println("Transactions executed: " + transactionNum);
        System.out.printf("Net cash: %.2f%n%n", cash);

    }

    //Adjusts all dates to have values consistent with splits
    static void splitAdjust(ArrayList<Date> tradingDays, ArrayList<Date> splitDays){
        /*for(int i = 0; i < tradingDays.size(); i++){
            System.out.println(tradingDays.get(i));
        }*/
        double splitType, divisor = 1;
        for(int i = 0; i < tradingDays.size() - 1; i++){
            splitType = 1;
            splitType = checkSplit(tradingDays.get(i), tradingDays.get(i+1), divisor, splitDays);
            divisor *= splitType;
            tradingDays.get(i+1).divideBy(divisor);
        }
    }

    //Checks if a split occurs and returns the type. Adds all dates that a split occurs on to an ArrayList
    static double checkSplit(Date next, Date curr, double divisor, ArrayList<Date> splitDays){
        double split = ((curr.getClosePrice()/divisor)/next.getOpenPrice());
        double splitType;
        if(Math.abs(split - 2.0) < .2){
            splitType = 2;
            splitDays.add(new Date(curr.getDate(), curr.getClosePrice(), next.getOpenPrice()*divisor, splitType));
        }
        else if(Math.abs(split - 3.0) < .3){
            splitType = 3;
            splitDays.add(new Date(curr.getDate(), curr.getClosePrice(), next.getOpenPrice()*divisor, splitType));
        }
        else if(Math.abs(split - 1.5) < .15){
            splitType = 1.5;
            splitDays.add(new Date(curr.getDate(), curr.getClosePrice(), next.getOpenPrice()*divisor, splitType));
        }
        else{
            splitType = 1;
        }
        return splitType;
    }
}