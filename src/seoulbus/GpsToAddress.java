package seoulbus;

    /**
     * # 버스 정류정 위치 정보
     * - https://data.seoul.go.kr/dataList/OA-15067/S/1/datasetView.do
     * seoul_bus_gps = pd.read_csv('2021_seoul_bus_gps.csv', encoding='euc-kr')
     * seoul_bus_gps.head(2)
     *
     * url_id_ori = 'https://dapi.kakao.com/v2/local/geo/coord2address.json?x={}&y={}&input_coord=WGS84'
     * url_id = url_id_ori.format(126.987786, 37.569764)
     * url_id
     *
     * 'https://dapi.kakao.com/v2/local/geo/coord2address.json?x=126.987786&y=37.569764&input_coord=WGS84'
     *
     *
     */
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.io.BufferedReader;
import java.io.IOException;

public class GpsToAddress {

    public static void main(String[] args) throws IOException {

        String urlIdOri = "https://dapi.kakao.com/v2/local/geo/coord2address.json?x={}&y={}&input_coord=WGS84";
        String urlId = String.format(urlIdOri, 126.987786, 37.569764);
        System.out.println(urlId);
        }
}

