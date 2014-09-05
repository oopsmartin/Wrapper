import com.qunar.qfwrapper.bean.booking.BookingResult;
import com.qunar.qfwrapper.bean.booking.BookingInfo;
import com.qunar.qfwrapper.bean.search.FlightSearchParam;
import com.qunar.qfwrapper.bean.search.ProcessResultInfo;
import com.qunar.qfwrapper.bean.search.OneWayFlightInfo;
import com.qunar.qfwrapper.bean.search.FlightDetail;
import com.qunar.qfwrapper.bean.search.FlightSegement;
import com.qunar.qfwrapper.bean.search.RoundTripFlightInfo;
import com.qunar.qfwrapper.interfaces.QunarCrawler;
import com.qunar.qfwrapper.util.QFGetMethod;
import com.qunar.qfwrapper.util.QFHttpClient;
import com.qunar.qfwrapper.constants.Constants;

import org.apache.commons.httpclient.cookie.CookiePolicy;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

import org.apache.commons.lang.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;

public class Wrapper_gjdaire8001 implements QunarCrawler{

	public static void main(String[] args) {

	    RoundTripFlightInfo test = new RoundTripFlightInfo();
		FlightSearchParam searchParam = new FlightSearchParam();
		searchParam.setDep("HKG");
		searchParam.setArr("HKT");
		searchParam.setDepDate("2014-08-12");
		searchParam.setTimeOut("60000");
		searchParam.setToken("");
		
		String html = new  Wrapper_gjdaire8001().getHtml(searchParam);

		ProcessResultInfo result = new ProcessResultInfo();
		result = new  Wrapper_gjdaire8001().process(html,searchParam);
		if(result.isRet() && result.getStatus().equals(Constants.SUCCESS))
		{
			List<OneWayFlightInfo> flightList = (List<OneWayFlightInfo>) result.getData();
			for (OneWayFlightInfo in : flightList){
				System.out.println("************" + in.getInfo().toString());
				System.out.println("++++++++++++" + in.getDetail().toString());
			}
		}
		else
		{
			System.out.println(result.getStatus());
		}
	}
	
	public BookingResult getBookingInfo(FlightSearchParam arg0) {

		String bookingUrlPre = "http://ashley4.com/webaccess/cityairways/fareresult.php";
		BookingResult bookingResult = new BookingResult();
		
		BookingInfo bookingInfo = new BookingInfo();
		bookingInfo.setAction(bookingUrlPre);
		bookingInfo.setMethod("post");
		Map<String, String> map = new LinkedHashMap<String, String>();
		map.put("ro", "0");
		map.put("from", arg0.getDep());
		map.put("to", arg0.getArr());
		map.put("cur", "HKD");
		map.put("sdate", arg0.getDepDate().replaceAll("-", "/"));
		map.put("edate", arg0.getDepDate().replaceAll("-", "/"));
		map.put("adult", "1");
		map.put("child", "0");
		map.put("infant", "0");
		map.put("view", "0");
		map.put("btnsubmit", "Flight Search");
		bookingInfo.setInputs(map);
		bookingResult.setData(bookingInfo);
		bookingResult.setRet(true);
		return bookingResult;

	}

	public String getHtml(FlightSearchParam arg0) {
		QFGetMethod get = null;	
		try {	
		QFHttpClient httpClient = new QFHttpClient(arg0, false);
		
		/*瀵逛簬闇�cookie鐨勭綉绔欙紝璇疯嚜宸卞鐞哻ookie锛堝繀椤伙級
		* 渚嬪锛�
		* httpClient.getParams().setCookiePolicy(CookiePolicy.BROWSER_COMPATIBILITY);
		*/

		String getUrl = String.format("http://ashley4.com/webaccess/cityairways/getfs.php?cur=HKD&from=%s&to=%s&adult=1&child=0&infant=0&sdate=%s&sdate=%s&ro=0", arg0.getDep(), arg0.getArr(), arg0.getDepDate().replaceAll("-", "/"), arg0.getDepDate());
	
			
			get = new QFGetMethod(getUrl);
			
			//1銆佸浜庨�杩囧娆et|post璇锋眰鎵嶈兘寰楀埌鍖呭惈鏈虹エ淇℃伅鐨勭綉绔欙紝闇�娉ㄦ剰瀵箂tatus鐨勫垽鏂�
			//2銆佸浜庨�杩囧娆et|post璇锋眰鎵嶈兘寰楀埌鍖呭惈鏈虹エ淇℃伅鐨勭綉绔欙紝濡傛灉闇�cookie锛屽垯鍦ㄦ瘡涓�get|post璇锋眰鍓嶉兘澶勭悊濂絚ookie
			//3銆佸鏋滅綉绔欓渶瑕佷娇鐢╟ookie锛孏etMethod 閬囧埌 302 鏃堕粯璁や細鑷姩璺宠浆锛屼笉鐣欐満浼氱粰 寮�彂澶勭悊Cookie锛岃繖涓椂鍊欒鐗瑰埆灏忓績锛�闇�浣跨敤 get.setFollowRedirects(false); 闃绘鑷姩璺宠浆锛岀劧鍚庤嚜宸卞鐞�02 浠ュ強Cookie銆�
			/* 渚嬪锛�
			try {
				get.setFollowRedirects(false);
				get.getParams().setContentCharset("utf-8");
				client.executeMethod(get);
			
				if(get.getStatusCode() == HttpStatus.SC_MOVED_TEMPORARILY || get.getStatusCode() == HttpStatus.SC_MOVED_PERMANENTLY){
					Header location = get.getResponseHeader("Location");
					String url = "";
					if(location !=null){
						url = location.getValue();
						if(!url.startsWith("http")){
							url = get.getURI().getScheme() + "://" + get.getURI().getHost() + (get.getURI().getPort()==-1?"":(":"+get.getURI().getPort())) + url;
						}
					}else{
						return;
					}
					String cookie = StringUtils.join(client.getState().getCookies(),"; ");
					get = new QFGetMethod(url);
					client.getState().clearCookies();
					get.addRequestHeader("Cookie",cookie);
					client.executeMethod(get);
				}
			} catch (Exception e) {
			e.printStackTrace();
			} finally {
				if(get!=null){
					get.releaseConnection();
				}
			}
			 */
		    int status = httpClient.executeMethod(get);
		    
		    return get.getResponseBodyAsString();

		} catch (Exception e) {			
			e.printStackTrace();
		} finally{
			if (null != get){
				get.releaseConnection();
			}
		}
		return "Exception";
	}


	public ProcessResultInfo process(String arg0, FlightSearchParam arg1) {
		String html = arg0;
		
		/* ProcessResultInfo涓紝
		 * ret涓簍rue鏃讹紝status鍙互涓猴細SUCCESS(鎶撳彇鍒版満绁ㄤ环鏍�|NO_RESULT(鏃犵粨鏋滐紝娌℃湁鍙崠鐨勬満绁�
		 * ret涓篺alse鏃讹紝status鍙互涓�CONNECTION_FAIL|INVALID_DATE|INVALID_AIRLINE|PARSING_FAIL|PARAM_ERROR
		 */
		ProcessResultInfo result = new ProcessResultInfo();
		if ("Exception".equals(html)) {	
			result.setRet(false);
			result.setStatus(Constants.CONNECTION_FAIL);
			return result;			
		}		
		//闇�鏈夋槑鏄剧殑鎻愮ず璇彞锛屾墠鑳藉垽鏂槸鍚NVALID_DATE|INVALID_AIRLINE|NO_RESULT
		if (html.contains("Today Flight is full, select an other day or check later for any seat released. ")) {
			result.setRet(false);
			result.setStatus(Constants.INVALID_DATE);
			return result;			
		}
		

		String jsonStr = org.apache.commons.lang.StringUtils.substringBetween(html, "var json = '", "';");		
		try {			
			List<OneWayFlightInfo> flightList = new ArrayList<OneWayFlightInfo>();
			JSONArray ajson = JSON.parseArray(jsonStr);				
			for (int i = 0; i < ajson.size(); i++){
				OneWayFlightInfo baseFlight = new OneWayFlightInfo();
				List<FlightSegement> segs = new ArrayList<FlightSegement>();
				FlightDetail flightDetail = new FlightDetail();
				FlightSegement seg = new FlightSegement();
				List<String> flightNoList = new ArrayList<String>();
				JSONObject ojson = ajson.getJSONObject(i);
				String flightNo = ojson.getString("flight").replaceAll("[^a-zA-Z\\d]", "");
				flightNoList.add(flightNo);
				seg.setFlightno(flightNo);
				seg.setDepDate(ojson.getString("date"));
				seg.setDepairport(ojson.getString("org"));
				seg.setArrairport(ojson.getString("dst"));
				seg.setDeptime(ojson.getString("dep"));
				seg.setArrtime(ojson.getString("arr"));
				flightDetail.setDepdate(ojson.getDate("date"));
				JSONArray classArray = ojson.getJSONArray("class");
				double price = 0;
				String cur = "";				
				for(int j = 0; j < classArray.size(); j++){
					JSONObject jsonObject = classArray.getJSONObject(j);
					if (StringUtils.isEmpty(cur)){
						cur = jsonObject.getString("cur");
					}
					double tmpPrice = jsonObject.getDouble("adult");
					if (0 == price || price > tmpPrice){
						price = tmpPrice;
					}
				}
				flightDetail.setFlightno(flightNoList);
				flightDetail.setMonetaryunit(cur);
				flightDetail.setPrice(price);
				flightDetail.setDepcity(arg1.getDep());
				flightDetail.setArrcity(arg1.getArr());
				flightDetail.setWrapperid(arg1.getWrapperid());
				segs.add(seg);
				baseFlight.setDetail(flightDetail);
				baseFlight.setInfo(segs);
				flightList.add(baseFlight);
			}	
			result.setRet(true);
			result.setStatus(Constants.SUCCESS);
			result.setData(flightList);
			return result;
		} catch(Exception e){
			result.setRet(false);
			result.setStatus(Constants.PARSING_FAIL);
			return result;
		}
	}
	
}
