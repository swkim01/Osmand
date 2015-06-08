package net.osmand.map;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.URL;
import java.net.URLConnection;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import net.osmand.PlatformUtil;
import net.osmand.osm.io.NetworkUtils;
import net.osmand.util.Algorithms;
import net.osmand.util.Bd02MapUtils;
import net.osmand.util.DaumMapUtils;
import net.osmand.util.ForestOnMapUtils;
import net.osmand.util.Gcj09MapUtils;
import net.osmand.util.MapUtils;
import net.osmand.util.NaverMapUtils;
import net.osmand.util.SogouMapUtils;
import net.osmand.util.YahooMapUtils;

import org.apache.commons.logging.Log;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import bsh.Interpreter;


public class TileSourceManager {
	private static final Log log = PlatformUtil.getLog(TileSourceManager.class);
	public static final String RULE_BEANSHELL = "beanshell";
	public static final String RULE_YANDEX_TRAFFIC = "yandex_traffic";
	private static final String RULE_WMS = "wms_tile";

	private static final String RULE_YAHOO = "yahoo";
	private static final String RULE_DAUM = "daum";
	private static final String RULE_NAVER = "naver";
	private static final String RULE_FORESTON = "foreston";
	private static final String RULE_GCJ09 = "gcj09";
	private static final String RULE_BAIDU = "baidu";
	private static final String RULE_SOGOU = "sogou";

	public static MapUtils[] mapUtilsList = {new MapUtils(), new YahooMapUtils(), new DaumMapUtils(), new NaverMapUtils(), new ForestOnMapUtils(), new Gcj09MapUtils(), new Bd02MapUtils(), new SogouMapUtils()};

	public static class TileSourceTemplate implements ITileSource, Cloneable {
		private int maxZoom;
		private int minZoom;
		private String name;
		protected int tileSize;
		protected String urlToLoad;
		protected String ext;
		private int avgSize;
		private int bitDensity;
		// -1 never expires, 
		private int expirationTimeMillis = -1;
		private boolean ellipticYTile;
		private String rule;
		
		private boolean isRuleAcceptable = true;
		
		private MapUtils mapUtil = mapUtilsList[0];
                private int vIndexOrder = 1;
		
		public TileSourceTemplate(String name, String urlToLoad, String ext, int maxZoom, int minZoom, int tileSize, int bitDensity,
				int avgSize) {
			this.maxZoom = maxZoom;
			this.minZoom = minZoom;
			this.name = name;
			this.tileSize = tileSize;
			this.urlToLoad = urlToLoad;
			this.ext = ext;
			this.avgSize = avgSize;
			this.bitDensity = bitDensity;
		}
		
		public static String normalizeUrl(String url){
			if(url != null){
				url = url.replaceAll("\\{\\$z\\}", "{0}"); //$NON-NLS-1$ //$NON-NLS-2$
				url = url.replaceAll("\\{\\$x\\}", "{1}"); //$NON-NLS-1$//$NON-NLS-2$
				url = url.replaceAll("\\{\\$y\\}", "{2}"); //$NON-NLS-1$ //$NON-NLS-2$
			}
			return url;
		}
		public void setMinZoom(int minZoom) {
			this.minZoom = minZoom;
		}
		
		public void setMaxZoom(int maxZoom) {
			this.maxZoom = maxZoom;
		}
		
		
		public void setName(String name) {
			this.name = name;
		}

		public void setEllipticYTile(boolean ellipticYTile) {
			this.ellipticYTile = ellipticYTile;
		}

		@Override
		public boolean isEllipticYTile() {
			return ellipticYTile;
		}

		@Override
		public int getBitDensity() {
			return bitDensity;
		}

		public int getAverageSize() {
			return avgSize;
		}

		@Override
		public int getMaximumZoomSupported() {
			return maxZoom;
		}

		@Override
		public int getMinimumZoomSupported() {
			return minZoom;
		}

		@Override
		public String getName() {
			return name;
		}
		
		public void setExpirationTimeMillis(int timeMillis) {
			this.expirationTimeMillis = timeMillis;
		}
		
		public void setExpirationTimeMinutes(int minutes) {
			if(minutes < 0) {
				this.expirationTimeMillis = -1;
			} else {
				this.expirationTimeMillis = minutes * 60 * 1000;
			}
		}
		
		public int getExpirationTimeMinutes() {
			if(expirationTimeMillis < 0) {
				return -1;
			}
			return expirationTimeMillis / (60  * 1000);
		}
		
		public int getExpirationTimeMillis() {
			return expirationTimeMillis;
		}
		

		@Override
		public int getTileSize() {
			return tileSize;
		}

		@Override
		public String getTileFormat() {
			return ext;
		}
		
		public void setTileFormat(String ext) {
			this.ext = ext;
		}
		
		public void setUrlToLoad(String urlToLoad) {
			this.urlToLoad = urlToLoad;
		}
		
		public boolean isRuleAcceptable() {
			return isRuleAcceptable;
		}
		
		public void setRuleAcceptable(boolean isRuleAcceptable) {
			this.isRuleAcceptable = isRuleAcceptable;
		}
		
		public TileSourceTemplate copy() {
			try {
				return (TileSourceTemplate) this.clone();
			} catch (CloneNotSupportedException e) {
				return this;
			}
		}

		@Override
		public String getUrlToLoad(int x, int y, int zoom) {
			// use int to string not format numbers! (non-nls)
			if (urlToLoad == null) {
				return null;
			}
			return MessageFormat.format(urlToLoad, zoom + "", x + "", y + ""); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		}

		public String getUrlTemplate() {
			return urlToLoad;
		}

		@Override
		public boolean couldBeDownloadedFromInternet() {
			return urlToLoad != null;
		}
		
		
		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((name == null) ? 0 : name.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			TileSourceTemplate other = (TileSourceTemplate) obj;
			if (name == null) {
				if (other.name != null)
					return false;
			} else if (!name.equals(other.name))
				return false;
			return true;
		}

		public void setRule(String rule) {
			this.rule = rule;
		}

		public String getRule() {
			return rule;
		}

		public void setMapUtil(int index) {
			this.mapUtil = mapUtilsList[index];
		}
		
		@Override
		public MapUtils getMapUtils() {
			return mapUtil;
		}
		
		public String calculateTileId(int x, int y, int zoom) {
			StringBuilder builder = new StringBuilder(getName());
			builder.append('/');
			builder.append(zoom).append('/').append(x).append('/').append(y).append(getTileFormat()).append(".tile"); //$NON-NLS-1$ //$NON-NLS-2$
			return builder.toString();
		}
		
		@Override
		public byte[] getBytes(int x, int y, int zoom, String dirWithTiles) throws IOException {
			File f = new File(dirWithTiles, calculateTileId(x, y, zoom));
			if (!f.exists())
				return null;
			
			ByteArrayOutputStream bous = new ByteArrayOutputStream();
			FileInputStream fis = new FileInputStream(f);
			Algorithms.streamCopy(fis, bous);
			fis.close();
			bous.close();
			return bous.toByteArray();
		}
	}
	
	private static Map<String, String> readMetaInfoFile(File dir) {
		Map<String, String> keyValueMap = new LinkedHashMap<String, String>();
		try {

			File metainfo = new File(dir, ".metainfo"); //$NON-NLS-1$
			if (metainfo.exists()) {
				BufferedReader reader = new BufferedReader(new InputStreamReader(
						new FileInputStream(metainfo), "UTF-8")); //$NON-NLS-1$
				String line;
				String key = null;
				while ((line = reader.readLine()) != null) {
					line = line.trim();
					if (line.startsWith("[")) {
						key = line.substring(1, line.length() - 1).toLowerCase();
					} else if (key != null && line.length() > 0) {
						keyValueMap.put(key, line);
						key = null;
					}
				}
				reader.close();
			}
		} catch (IOException e) {
			log.error("Error reading metainfo file " + dir.getAbsolutePath(), e);
		}
		return keyValueMap;
	}	
	
	private static int parseInt(Map<String, String> attributes, String value, int def){
		String val = attributes.get(value);
		if(val == null){
			return def;
		}
		try {
			return Integer.parseInt(val);
		} catch (NumberFormatException e) {
			return def;
		}
	}
	
	public static void createMetaInfoFile(File dir, TileSourceTemplate tm, boolean override) throws IOException {
		File metainfo = new File(dir, ".metainfo"); //$NON-NLS-1$
		Map<String, String> properties = new LinkedHashMap<String, String>();
		if (tm.getRule() != null && tm.getRule().length() > 0) {
			properties.put("rule", tm.getRule());
		}
		if(tm.getUrlTemplate() != null) {
			properties.put("url_template", tm.getUrlTemplate());
		}

		properties.put("ext", tm.getTileFormat());
		properties.put("min_zoom", tm.getMinimumZoomSupported() + "");
		properties.put("max_zoom", tm.getMaximumZoomSupported() + "");
		properties.put("tile_size", tm.getTileSize() + "");
		properties.put("img_density", tm.getBitDensity() + "");
		properties.put("avg_img_size", tm.getAverageSize() + "");

		if (tm.isEllipticYTile()) {
			properties.put("ellipsoid", tm.isEllipticYTile() + "");
		}
		if (tm.getExpirationTimeMinutes() != -1) {
			properties.put("expiration_time_minutes", tm.getExpirationTimeMinutes() + "");
		}
		if (override || !metainfo.exists()) {
			BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(metainfo)));
			for (String key : properties.keySet()) {
				writer.write("[" + key + "]\n" + properties.get(key) + "\n");
			}
			writer.close();
		}
	}
	
	public static boolean isTileSourceMetaInfoExist(File dir){
		return new File(dir, ".metainfo").exists() || new File(dir, "url").exists();
	}
	
	/**
	 * @param dir
	 * @return doesn't return null 
	 */
	public static TileSourceTemplate createTileSourceTemplate(File dir) {
		// read metainfo file
		Map<String, String> metaInfo = readMetaInfoFile(dir);
		boolean ruleAcceptable = true;
		if(!metaInfo.isEmpty()){
			metaInfo.put("name", dir.getName());
			TileSourceTemplate template = createTileSourceTemplate(metaInfo);
			if(template != null){
				return template;
			}
			ruleAcceptable = false;
		}
		
		// try to find url
		String ext = findOneTile(dir);
		ext = ext == null ? ".jpg" : ext;
		String url = null;
			File readUrl = new File(dir, "url"); //$NON-NLS-1$
			try {
				if (readUrl.exists()) {
					BufferedReader reader = new BufferedReader(new InputStreamReader(
							new FileInputStream(readUrl), "UTF-8")); //$NON-NLS-1$
					url = reader.readLine();
					// 
					//url = url.replaceAll("\\{\\$z\\}", "{0}"); //$NON-NLS-1$ //$NON-NLS-2$
					//url = url.replaceAll("\\{\\$x\\}", "{1}"); //$NON-NLS-1$//$NON-NLS-2$
					//url = url.replaceAll("\\{\\$y\\}", "{2}"); //$NON-NLS-1$ //$NON-NLS-2$
					url = TileSourceTemplate.normalizeUrl(url);
					reader.close();
				}
			} catch (IOException e) {
				log.debug("Error reading url " + dir.getName(), e); //$NON-NLS-1$
			}

		TileSourceTemplate template = new TileSourceManager.TileSourceTemplate(dir.getName(), url,
				ext, 18, 1, 256, 16, 20000); //$NON-NLS-1$
		template.setRuleAcceptable(ruleAcceptable);
		return template;
	}

	private static String findOneTile(File dir) {
		if (dir.isDirectory()) {
			for (File file : dir.listFiles()) {
				if (file.isDirectory()) {
					String ext = findOneTile(file);
					if (ext != null) {
						return ext;
					}
				} else {
					String fileName = file.getName();
					if (fileName.endsWith(".tile")) {
						String substring = fileName.substring(0, fileName.length() - ".tile".length());
						int extInt = substring.lastIndexOf('.');
						if (extInt != -1) {
							return substring.substring(extInt, substring.length());
						}
					}
				}
			}
		}
		return null;
	}
	
	public static java.util.List<TileSourceTemplate> getKnownSourceTemplates() {
		java.util.List<TileSourceTemplate> list = new ArrayList<TileSourceTemplate>();
		list.add(getMapnikSource());
		list.add(getCycleMapSource());
		return list;

	}

	public static TileSourceTemplate getMapnikSource(){
		return new TileSourceTemplate("OsmAnd (online tiles)", "http://tile.osmand.net/hd/{0}/{1}/{2}.png", ".png", 19, 1, 512, 8, 18000);  //$NON-NLS-1$//$NON-NLS-2$ //$NON-NLS-3$
	}

	public static TileSourceTemplate getCycleMapSource(){
		return new TileSourceTemplate("CycleMap", "http://b.tile.opencyclemap.org/cycle/{0}/{1}/{2}.png", ".png", 16, 1, 256, 32, 18000);  //$NON-NLS-1$//$NON-NLS-2$ //$NON-NLS-3$
	}


	public static List<TileSourceTemplate> downloadTileSourceTemplates(String versionAsUrl) {
		try {
			URLConnection connection = NetworkUtils.getHttpURLConnection("http://download.osmand.net//tile_sources.php?" + versionAsUrl);
			return createTileSourceTemplates(connection.getInputStream());
		} catch (IOException e) {
			log.error("Exception while downloading tile sources", e);
			return null;
		}
	}
	
	public static List<TileSourceTemplate> getLocalTileSourceTemplates(File tilesDir) {
		try {
			File customTiles = new File(tilesDir, "custom_tile_sources.xml");
return createTileSourceTemplates(new FileInputStream(customTiles));
		} catch (FileNotFoundException e) {
			log.info("Geting local tile sources: No custom file specified (" + tilesDir.getAbsolutePath() + File.separator + "custom_tile_sources.xml" + ")");
			return null;
		}
	}
	
	private static List<TileSourceTemplate> createTileSourceTemplates(InputStream inputStream) {

		final List<TileSourceTemplate> templates = new ArrayList<TileSourceTemplate>();
		try {
			XmlPullParser parser = PlatformUtil.newXMLPullParser();
			parser.setInput(inputStream, "UTF-8");
			int tok;
			while ((tok = parser.next()) != XmlPullParser.END_DOCUMENT) {
				if (tok == XmlPullParser.START_TAG) {
					String name = parser.getName();
					if (name.equals("tile_source")) {
						Map<String, String> attrs = new LinkedHashMap<String, String>();
						for(int i=0; i< parser.getAttributeCount(); i++) {
							attrs.put(parser.getAttributeName(i), parser.getAttributeValue(i));
						}
						TileSourceTemplate template = createTileSourceTemplate(attrs);
						if (template != null) {
							templates.add(template);
						}
					}
				}
			}
		} catch (IOException e) {
			log.error("Exception while downloading tile sources", e);
			return null;
		} catch (XmlPullParserException e) {
			log.error("Exception while downloading tile sources", e);
			return null;
		}
		return templates;
	}
	
	private static TileSourceTemplate createTileSourceTemplate(Map<String, String> attrs) {
		TileSourceTemplate template = null;
		String rule = attrs.get("rule");
		if(rule == null){
			template = createSimpleTileSourceTemplate(attrs, false);
		} else if(RULE_BEANSHELL.equalsIgnoreCase(rule)){
			template = createBeanshellTileSourceTemplate(attrs);
		} else if (RULE_WMS.equalsIgnoreCase(rule)) {
			template = createWmsTileSourceTemplate(attrs);
		} else if (RULE_YANDEX_TRAFFIC.equalsIgnoreCase(rule)) {
			template = createSimpleTileSourceTemplate(attrs, true);
		} else if (RULE_DAUM.equalsIgnoreCase(rule)) {
			template = createDaumTileSourceTemplate(attrs, true);
		} else if (RULE_NAVER.equalsIgnoreCase(rule)) {
			template = createNaverTileSourceTemplate(attrs, true);
		} else if (RULE_FORESTON.equalsIgnoreCase(rule)) {
			template = createForestOnTileSourceTemplate(attrs, true);
		} else if (RULE_YAHOO.equalsIgnoreCase(rule)) {
			template = createYahooTileSourceTemplate(attrs, true);
		} else if (RULE_GCJ09.equalsIgnoreCase(rule)) {
			template = createGcj09TileSourceTemplate(attrs, true);
		} else if (RULE_BAIDU.equalsIgnoreCase(rule)) {
			template = createBaiduTileSourceTemplate(attrs, true);
		} else if (RULE_SOGOU.equalsIgnoreCase(rule)) {
			template = createSogouTileSourceTemplate(attrs, true);
		} else {
			return null;
		}
		if(template != null){
			template.setRule(rule);
		}
		return template;
	}
	
	
	private static TileSourceTemplate createWmsTileSourceTemplate(Map<String, String> attributes) {
		String name = attributes.get("name");
		String layer = attributes.get("layer");
		String urlTemplate = attributes.get("url_template");
		
		if (name == null || urlTemplate == null || layer == null) {
			return null;
		}
		int maxZoom = parseInt(attributes, "max_zoom", 18);
		int minZoom = parseInt(attributes, "min_zoom", 5);
		int tileSize = parseInt(attributes, "tile_size", 256);
		String ext = attributes.get("ext") == null ? ".jpg" : attributes.get("ext");
		int bitDensity = parseInt(attributes, "img_density", 16);
		int avgTileSize = parseInt(attributes, "avg_img_size", 18000);
		urlTemplate = " http://whoots.mapwarper.net/tms/{0}/{1}/{2}/"+layer+"/"+urlTemplate;
		TileSourceTemplate templ = new TileSourceTemplate(name, urlTemplate, ext, maxZoom, minZoom, tileSize, bitDensity, avgTileSize);
		return templ;
	}
	


	private static TileSourceTemplate createSimpleTileSourceTemplate(Map<String, String> attributes, boolean ignoreTemplate) {
		String name = attributes.get("name");
		String urlTemplate = attributes.get("url_template");
		if (name == null || (urlTemplate == null && !ignoreTemplate)) {
			return null;
		}
		//As I see, here is no changes to urlTemplate  
		//if(urlTemplate != null){
			//urlTemplate.replace("${x}", "{1}").replace("${y}", "{2}").replace("${z}", "{0}");
		//}
		urlTemplate = TileSourceTemplate.normalizeUrl(urlTemplate);

		int maxZoom = parseInt(attributes, "max_zoom", 18);
		int minZoom = parseInt(attributes, "min_zoom", 5);
		int tileSize = parseInt(attributes, "tile_size", 256);
		int expirationTime = parseInt(attributes, "expiration_time_minutes", -1);
		String ext = attributes.get("ext") == null ? ".jpg" : attributes.get("ext");
		int bitDensity = parseInt(attributes, "img_density", 16);
		int avgTileSize = parseInt(attributes, "avg_img_size", 18000);
		boolean ellipsoid = false;
		if (Boolean.parseBoolean(attributes.get("ellipsoid"))) {
			ellipsoid = true;
		}
		TileSourceTemplate templ = new TileSourceTemplate(name, urlTemplate, ext, maxZoom, minZoom, tileSize, bitDensity, avgTileSize);
		if(expirationTime >= 0) {
			templ.setExpirationTimeMinutes(expirationTime);
		}
		templ.setEllipticYTile(ellipsoid);
		return templ;
	}
	
	private static TileSourceTemplate createBeanshellTileSourceTemplate(Map<String, String> attributes) {
		String name = attributes.get("name");
		String urlTemplate = attributes.get("url_template");
		if (name == null || urlTemplate == null) {
			return null;
		}
		int maxZoom = parseInt(attributes, "max_zoom", 18);
		int minZoom = parseInt(attributes, "min_zoom", 5);
		int tileSize = parseInt(attributes, "tile_size", 256);
		String ext = attributes.get("ext") == null ? ".jpg" : attributes.get("ext");
		int bitDensity = parseInt(attributes, "img_density", 16);
		int avgTileSize = parseInt(attributes, "avg_img_size", 18000);
		int expirationTime = parseInt(attributes, "expiration_time_minutes", -1);
		boolean ellipsoid = false;
		if (Boolean.parseBoolean(attributes.get("ellipsoid"))) {
			ellipsoid = true;
		}
		TileSourceTemplate templ;
		templ = new BeanShellTileSourceTemplate(name, urlTemplate, ext, maxZoom, minZoom, tileSize, bitDensity, avgTileSize);
		templ.setEllipticYTile(ellipsoid);
		if(expirationTime > 0) {
			templ.setExpirationTimeMinutes(expirationTime);
		}
		return templ;
	}
	
	public static class BeanShellTileSourceTemplate extends TileSourceTemplate {

		Interpreter bshInterpreter;
		
		public BeanShellTileSourceTemplate(String name, String urlToLoad, String ext,
				int maxZoom, int minZoom, int tileSize, int bitDensity, int avgSize) {
			super(name, urlToLoad, ext, maxZoom, minZoom, tileSize, bitDensity, avgSize);
			bshInterpreter = new Interpreter();
			try {
				bshInterpreter.eval(urlToLoad);
				bshInterpreter.getClassManager().setClassLoader(new ClassLoader() {
					@Override
					public URL getResource(String resName) {
						return null;
					}
					
					@Override
					public InputStream getResourceAsStream(String resName) {
						return null;
					}
					
					@Override
					public Class<?> loadClass(String className) throws ClassNotFoundException {
						throw new ClassNotFoundException("Error requesting " + className);
					}
				});
			} catch (bsh.EvalError e) {
				log.error("Error executing the map init script " + urlToLoad, e);
			}
		}

		@Override
		public String getUrlToLoad(int x, int y, int zoom) {
			try {
				return (String) bshInterpreter.eval("getTileUrl("+zoom+","+x+","+y+");");
			} catch (bsh.EvalError e) {
				log.error(e.getMessage(), e);
				return null;
			}
		}
		
	}
	
	private static TileSourceTemplate createYahooTileSourceTemplate(Map<String, String> attributes, boolean ignoreTemplate) {
		String name = attributes.get("name");
		String urlTemplate = attributes.get("url_template");
		if (name == null || (urlTemplate == null && !ignoreTemplate)) {
			return null;
		}
		if(urlTemplate != null){
			urlTemplate.replace("${x}", "{1}").replace("${y}", "{2}").replace("${z}", "{0}");
		}
		int maxZoom = parseInt(attributes, "max_zoom", 18);
		int minZoom = parseInt(attributes, "min_zoom", 5);
		int tileSize = parseInt(attributes, "tile_size", 256);
		String ext = attributes.get("ext") == null ? ".jpg" : attributes.get("ext");
		int bitDensity = parseInt(attributes, "img_density", 16);
		int avgTileSize = parseInt(attributes, "avg_img_size", 18000);
		TileSourceTemplate templ = new YahooTileSourceTemplate(name, urlTemplate, ext, maxZoom, minZoom, tileSize, bitDensity, avgTileSize);
		templ.setMapUtil(1);
		return templ;
	}

	public static class YahooTileSourceTemplate extends TileSourceTemplate {
		
		public YahooTileSourceTemplate(String name, String urlToLoad, String ext,
				int maxZoom, int minZoom, int tileSize, int bitDensity, int avgSize) {
			super(name, urlToLoad, ext, maxZoom, minZoom, tileSize, bitDensity, avgSize);
		}
		
		@Override
		public String getUrlToLoad(int x, int y, int zoom) {
			if(urlToLoad == null){
				return null;
			}
			return MessageFormat.format(urlToLoad, zoom+1+"", x+"", y+""); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		}
	}
	
	private static TileSourceTemplate createDaumTileSourceTemplate(Map<String, String> attributes, boolean ignoreTemplate) {
		String name = attributes.get("name");
		String urlTemplate = attributes.get("url_template");
		if (name == null || (urlTemplate == null && !ignoreTemplate)) {
			return null;
		}
		if(urlTemplate != null){
			urlTemplate.replace("${x}", "{1}").replace("${y}", "{2}").replace("${z}", "{0}");
		}
		int maxZoom = parseInt(attributes, "max_zoom", 18);
		int minZoom = parseInt(attributes, "min_zoom", 5);
		int tileSize = parseInt(attributes, "tile_size", 256);
		String ext = attributes.get("ext") == null ? ".jpg" : attributes.get("ext");
		int bitDensity = parseInt(attributes, "img_density", 16);
		int avgTileSize = parseInt(attributes, "avg_img_size", 18000);
		TileSourceTemplate templ = new DaumTileSourceTemplate(name, urlTemplate, ext, maxZoom, minZoom, tileSize, bitDensity, avgTileSize);
		templ.setMapUtil(2);
		return templ;
	}

	public static class DaumTileSourceTemplate extends TileSourceTemplate {
		
		public DaumTileSourceTemplate(String name, String urlToLoad, String ext,
				int maxZoom, int minZoom, int tileSize, int bitDensity, int avgSize) {
			super(name, urlToLoad, ext, maxZoom, minZoom, tileSize, bitDensity, avgSize);
		}
		
		@Override
		public String getUrlToLoad(int x, int y, int zoom) {
			if(urlToLoad == null){
				return null;
			}
			
			if (zoom < 6) {
				return null;
			} else {	
				return MessageFormat.format(urlToLoad, 20-zoom+"", x+"", y+""); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			}
		}
	}
	
	private static TileSourceTemplate createNaverTileSourceTemplate(Map<String, String> attributes, boolean ignoreTemplate) {
		String name = attributes.get("name");
		String urlTemplate = attributes.get("url_template");
		if (name == null || (urlTemplate == null && !ignoreTemplate)) {
			return null;
		}
		if(urlTemplate != null){
			urlTemplate.replace("${x}", "{1}").replace("${y}", "{2}").replace("${z}", "{0}");
		}
		int maxZoom = parseInt(attributes, "max_zoom", 18);
		int minZoom = parseInt(attributes, "min_zoom", 5);
		int tileSize = parseInt(attributes, "tile_size", 256);
		String ext = attributes.get("ext") == null ? ".jpg" : attributes.get("ext");
		int bitDensity = parseInt(attributes, "img_density", 16);
		int avgTileSize = parseInt(attributes, "avg_img_size", 18000);
		TileSourceTemplate templ = new NaverTileSourceTemplate(name, urlTemplate, ext, maxZoom, minZoom, tileSize, bitDensity, avgTileSize);
		templ.setMapUtil(3);
		return templ;
	}

	public static class NaverTileSourceTemplate extends TileSourceTemplate {
		
		public NaverTileSourceTemplate(String name, String urlToLoad, String ext,
				int maxZoom, int minZoom, int tileSize, int bitDensity, int avgSize) {
			super(name, urlToLoad, ext, maxZoom, minZoom, tileSize, bitDensity, avgSize);
		}
		
		@Override
		public String getUrlToLoad(int x, int y, int zoom) {
			if(urlToLoad == null){
				return null;
			}
			
			if (zoom < 6) {
				return null;
			} else {
				return MessageFormat.format(urlToLoad, zoom-5+"", x+"", y+""); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			}
		}
	}
	
	private static TileSourceTemplate createForestOnTileSourceTemplate(Map<String, String> attributes, boolean ignoreTemplate) {
		String name = attributes.get("name");
		String urlTemplate = attributes.get("url_template");
		if (name == null || (urlTemplate == null && !ignoreTemplate)) {
			return null;
		}
		if(urlTemplate != null){
			urlTemplate.replace("${x}", "{1}").replace("${y}", "{2}").replace("${z}", "{0}");
		}
		int maxZoom = parseInt(attributes, "max_zoom", 18);
		int minZoom = parseInt(attributes, "min_zoom", 5);
		int tileSize = parseInt(attributes, "tile_size", 256);
		String ext = attributes.get("ext") == null ? ".jpg" : attributes.get("ext");
		int bitDensity = parseInt(attributes, "img_density", 16);
		int avgTileSize = parseInt(attributes, "avg_img_size", 18000);
		TileSourceTemplate templ = new ForestOnTileSourceTemplate(name, urlTemplate, ext, maxZoom, minZoom, tileSize, bitDensity, avgTileSize);
		templ.setMapUtil(4);
		return templ;
	}

	public static class ForestOnTileSourceTemplate extends TileSourceTemplate {
		
		public ForestOnTileSourceTemplate(String name, String urlToLoad, String ext,
				int maxZoom, int minZoom, int tileSize, int bitDensity, int avgSize) {
			super(name, urlToLoad, ext, maxZoom, minZoom, tileSize, bitDensity, avgSize);
		}
		
		@Override
		public String getUrlToLoad(int x, int y, int zoom) {
			if(urlToLoad == null){
				return null;
			}
			
			if (zoom < 6) {
				return null;
			} else {
				if (this.getName().contains("Track")) {
					int s = 1<<(25-zoom);

					return MessageFormat.format(urlToLoad, s+"", x+"", y+""); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
				}
				else {
					int b = 20-zoom;
					String c="00000";

					return urlToLoad
							+c.substring(0,2-Integer.toString(b).length())+b+"/"
							+c.substring(0,5-Integer.toString(y).length())+y+"/"
							+"KR_"
							+c.substring(0,2-Integer.toString(b).length())+b+"_"
							+c.substring(0,5-Integer.toString(x).length())+x+"_"
							+c.substring(0,5-Integer.toString(y).length())+y+ext;	
				}
			}
		}
	}
	
	private static TileSourceTemplate createGcj09TileSourceTemplate(Map<String, String> attributes, boolean ignoreTemplate) {
		String name = attributes.get("name");
		String urlTemplate = attributes.get("url_template");
		if (name == null || (urlTemplate == null && !ignoreTemplate)) {
			return null;
		}
		if(urlTemplate != null){
			urlTemplate.replace("${x}", "{1}").replace("${y}", "{2}").replace("${z}", "{0}");
		}
		int maxZoom = parseInt(attributes, "max_zoom", 18);
		int minZoom = parseInt(attributes, "min_zoom", 5);
		int tileSize = parseInt(attributes, "tile_size", 256);
		String ext = attributes.get("ext") == null ? ".jpg" : attributes.get("ext");
		int bitDensity = parseInt(attributes, "img_density", 16);
		int avgTileSize = parseInt(attributes, "avg_img_size", 18000);
		TileSourceTemplate templ = new Gcj09TileSourceTemplate(name, urlTemplate, ext, maxZoom, minZoom, tileSize, bitDensity, avgTileSize);
		templ.setMapUtil(5);
		return templ;
	}

	public static class Gcj09TileSourceTemplate extends TileSourceTemplate {
		
		public Gcj09TileSourceTemplate(String name, String urlToLoad, String ext,
				int maxZoom, int minZoom, int tileSize, int bitDensity, int avgSize) {
			super(name, urlToLoad, ext, maxZoom, minZoom, tileSize, bitDensity, avgSize);
		}
	}
	
	private static TileSourceTemplate createBaiduTileSourceTemplate(Map<String, String> attributes, boolean ignoreTemplate) {
		String name = attributes.get("name");
		String urlTemplate = attributes.get("url_template");
		if (name == null || (urlTemplate == null && !ignoreTemplate)) {
			return null;
		}
		if(urlTemplate != null){
			urlTemplate.replace("${x}", "{1}").replace("${y}", "{2}").replace("${z}", "{0}");
		}
		int maxZoom = parseInt(attributes, "max_zoom", 18);
		int minZoom = parseInt(attributes, "min_zoom", 5);
		int tileSize = parseInt(attributes, "tile_size", 256);
		String ext = attributes.get("ext") == null ? ".jpg" : attributes.get("ext");
		int bitDensity = parseInt(attributes, "img_density", 16);
		int avgTileSize = parseInt(attributes, "avg_img_size", 18000);
		TileSourceTemplate templ = new BaiduTileSourceTemplate(name, urlTemplate, ext, maxZoom, minZoom, tileSize, bitDensity, avgTileSize);
		templ.setMapUtil(6);
		return templ;
	}
	
	public static class BaiduTileSourceTemplate extends TileSourceTemplate {
		
		public BaiduTileSourceTemplate(String name, String urlToLoad, String ext,
				int maxZoom, int minZoom, int tileSize, int bitDensity, int avgSize) {
			super(name, urlToLoad, ext, maxZoom, minZoom, tileSize, bitDensity, avgSize);
		}
		
		@Override
		public String getUrlToLoad(int x, int y, int zoom) {
			if(urlToLoad == null){
				return null;
			}
			return MessageFormat.format(urlToLoad, zoom+1+"", x+"", y+""); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		}
	}

	private static TileSourceTemplate createSogouTileSourceTemplate(Map<String, String> attributes, boolean ignoreTemplate) {
		String name = attributes.get("name");
		String urlTemplate = attributes.get("url_template");
		if (name == null || (urlTemplate == null && !ignoreTemplate)) {
			return null;
		}
		if(urlTemplate != null){
			urlTemplate.replace("${x}", "{1}").replace("${y}", "{2}").replace("${z}", "{0}");
		}
		int maxZoom = parseInt(attributes, "max_zoom", 18);
		int minZoom = parseInt(attributes, "min_zoom", 5);
		int tileSize = parseInt(attributes, "tile_size", 256);
		String ext = attributes.get("ext") == null ? ".jpg" : attributes.get("ext");
		int bitDensity = parseInt(attributes, "img_density", 16);
		int avgTileSize = parseInt(attributes, "avg_img_size", 18000);
		TileSourceTemplate templ = new SogouTileSourceTemplate(name, urlTemplate, ext, maxZoom, minZoom, tileSize, bitDensity, avgTileSize);
		templ.setMapUtil(7);
		return templ;
	}
	
	public static class SogouTileSourceTemplate extends TileSourceTemplate {
		
		public SogouTileSourceTemplate(String name, String urlToLoad, String ext,
				int maxZoom, int minZoom, int tileSize, int bitDensity, int avgSize) {
			super(name, urlToLoad, ext, maxZoom, minZoom, tileSize, bitDensity, avgSize);
		}

		static int areaid = 0;
		static int rmpid = 174;
		static int[] zoomIds = {728, 727, 726, 725, 724, 723, 722, 721, 720, 719, 718, 717, 716, 715, 714, 713, 712, 711, 792};
		
		@Override
		public String getUrlToLoad(int x, int y, int zoom) {
			if(urlToLoad == null){
				return null;
			}
			String x1;
			if (x >= 0) x1 = Integer.toString((int)Math.floor(x/200));
			else x1 = "M" + Math.abs((int)Math.floor(x/200));
			String y1;
			if (y >= 0) y1 = Integer.toString((int)Math.floor(y/200));
			else y1 = "M" + Math.abs((int)Math.floor(y/200));
			return MessageFormat.format(urlToLoad,
				zoomIds[zoom]+"",
				x1 + "/" + y1 + "/" +
				(x >= 0 ? x : ("M" + Math.abs(x))),
				(y >= 0 ? y : ("M" + Math.abs(y)))+""); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		}
	}
	
	
}
