package net.osmand.osm;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;

import net.osmand.PlatformUtil;
import net.osmand.data.AmenityType;
import net.osmand.osm.edit.OSMSettings.OSMTagKey;
import net.osmand.util.Algorithms;

import org.apache.commons.logging.Log;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

/**
 * reference : http://wiki.openstreetmap.org/wiki/Map_Features
 */
public class MapRenderingTypes {

	private static final Log log = PlatformUtil.getLog(MapRenderingTypes.class);
	
	public final static byte RESTRICTION_NO_RIGHT_TURN = 1;
	public final static byte RESTRICTION_NO_LEFT_TURN = 2;
	public final static byte RESTRICTION_NO_U_TURN = 3;
	public final static byte RESTRICTION_NO_STRAIGHT_ON = 4;
	public final static byte RESTRICTION_ONLY_RIGHT_TURN = 5;
	public final static byte RESTRICTION_ONLY_LEFT_TURN = 6;
	public final static byte RESTRICTION_ONLY_STRAIGHT_ON = 7;
	
	private static char TAG_DELIMETER = '/'; //$NON-NLS-1$
	
	private String resourceName = null;
	
	protected Map<String, MapRulType> types = null;
	protected List<MapRulType> typeList = new ArrayList<MapRulType>();
	protected MapRulType nameRuleType;
	protected MapRulType nameEnRuleType;

	public MapRenderingTypes(String fileName){
		this.resourceName = fileName;
	}
	
	private static MapRenderingTypes DEFAULT_INSTANCE = null;
	
	public static MapRenderingTypes getDefault() {
		if(DEFAULT_INSTANCE == null){
			DEFAULT_INSTANCE = new MapRenderingTypes(null);
		}
		return DEFAULT_INSTANCE;
	}

	public Map<String, MapRulType> getEncodingRuleTypes(){
		checkIfInitNeeded();
		return types;
	}
	
	
	protected void checkIfInitNeeded() {
		if(types == null) {
			types = new LinkedHashMap<String, MapRulType>();
			typeList.clear();
			nameRuleType = MapRulType.createText("name");
			nameRuleType.order = 40;
			registerRuleType(nameRuleType);
			nameEnRuleType = MapRulType.createText("name:en");
			nameEnRuleType.order = 45;
			registerRuleType(nameEnRuleType);
			init();
		}
	}
	public Collection<Map<String, String>> splitTagsIntoDifferentObjects(final Map<String, String> tags) {
		// check open sea maps tags
		boolean split = splitIsNeeded(tags);
		if(!split) {
			return Collections.singleton(tags);
		} else {
			return splitOpenSeaMapsTags(tags);
		}
	}

	protected boolean splitIsNeeded(final Map<String, String> tags) {
		boolean seamark = false;
		for(String s : tags.keySet()) {
			if(s.startsWith("seamark:")) {
				seamark = true;
				break;
			}
		}
		return seamark;
	}

	private Collection<Map<String, String>> splitOpenSeaMapsTags(final Map<String, String> tags) {
		Map<String, Map<String, String>> groupByOpenSeamaps = new HashMap<String, Map<String, String>>();
		Map<String, String> common = new HashMap<String, String>();
		String ATTACHED_KEY = "seamark:attached";
		String type = "";
		for (String s : tags.keySet()) {
			String value = tags.get(s);
			if (s.equals("seamark:type")) {
				type = value;
				common.put(ATTACHED_KEY, openSeaType(value));
			} else if (s.startsWith("seamark:")) {
				String stype = s.substring("seamark:".length());
				int ind = stype.indexOf(':');
				if (ind == -1) {
					common.put(s, value);
				} else {
					String group = openSeaType(stype.substring(0, ind));
					String add = stype.substring(ind + 1);
					if (!groupByOpenSeamaps.containsKey(group)) {
						groupByOpenSeamaps.put(group, new HashMap<String, String>());
					}
					groupByOpenSeamaps.get(group).put("seamark:" + add, value);
				}
			} else {
				common.put(s, value);
			}
		}
		List<Map<String, String>> res = new ArrayList<Map<String,String>>();
		for (Entry<String, Map<String, String>> g : groupByOpenSeamaps.entrySet()) {
			g.getValue().putAll(common);
			g.getValue().put("seamark", g.getKey());
			if (openSeaType(type).equals(g.getKey())) {
				g.getValue().remove(ATTACHED_KEY);
				g.getValue().put("seamark", type);
				res.add(0, g.getValue());
			} else {
				res.add(g.getValue());
			}
		}
		return res;
	}	
	
	
	private String openSeaType(String value) {
		if(value.equals("light_major") || value.equals("light_minor")) {
			return "light";
		}
		return value;
	}
	
	
	public MapRulType getTypeByInternalId(int id) {
		return typeList.get(id);
	}
	
	public MapRulType getAmenityRuleType(String tag, String val) {
		return getRuleType(tag, val, true);
	}
	
	private String lc(String a) {
		if(a != null) {
			return a.toLowerCase();
		}
		return a;
	}
	
	protected MapRulType getRuleType(String tag, String val, boolean poi) {
		Map<String, MapRulType> types = getEncodingRuleTypes();
		tag = lc(tag);
		val = lc(val);
		MapRulType rType = types.get(constructRuleKey(tag, val));
		if (rType == null || (!rType.isPOI() && poi) || (!rType.isMap() && !poi)) {
			rType = types.get(constructRuleKey(tag, null));
		}
		if(rType == null || (!rType.isPOI() && poi) || (!rType.isMap() && !poi)) {
			return null;
		} else if(rType.isAdditional() && rType.tagValuePattern.value == null) {
			MapRulType parent = rType;
			rType = MapRulType.createAdditional(tag, val);
			rType.additional = true;
			rType.order = parent.order;
			rType.onlyMap = parent.onlyMap;
			rType.onlyPoi = parent.onlyPoi;
			rType.onlyPoint = parent.onlyPoint;
			rType.poiSpecified = parent.poiSpecified;
			rType.poiCategory = parent.poiCategory;
			rType.poiPrefix = parent.poiPrefix;
			rType.poiWithNameOnly = parent.poiWithNameOnly;
			rType.namePrefix = parent.namePrefix;
			rType = registerRuleType(rType);
		}
		return rType;
	}
	
	public MapRulType getNameRuleType() {
		getEncodingRuleTypes();
		return nameRuleType;
	}
	
	public MapRulType getNameEnRuleType() {
		getEncodingRuleTypes();
		return nameEnRuleType;
	}
	
	public Map<String, String> getAmenityAdditionalInfo(Map<String, String> tags, AmenityType type, String subtype) {
		Map<String, String> map = new LinkedHashMap<String, String>();
		for (String tag : tags.keySet()) {
			String val = tags.get(tag);
			MapRulType rType = getAmenityRuleType(tag, val);
			if (rType != null && val != null && val.length()  > 0) {
				if(rType == nameEnRuleType && Algorithms.objectEquals(val, tags.get(OSMTagKey.NAME))) {
					continue;
				}
				if (rType.isAdditionalOrText()) {
					if (!rType.isText() && !Algorithms.isEmpty(rType.tagValuePattern.value)) {
						val = rType.tagValuePattern.value;
					}
					map.put(rType.tagValuePattern.tag, val);
				}
			}
		}
		return map;
	}
	
	public String getAmenitySubtype(String tag, String val){
		String prefix = getAmenitySubtypePrefix(tag, val);
		if(prefix != null){
			return prefix + val;
		}
		return val;
	}
	
	public String getAmenitySubtypePrefix(String tag, String val){
		Map<String, MapRulType> rules = getEncodingRuleTypes();
		MapRulType rt = rules.get(constructRuleKey(tag, val));
		if(rt != null && rt.poiPrefix != null && rt.isPOI()) {
			return rt.poiPrefix;
		}
		rt = rules.get(constructRuleKey(tag, null));
		if(rt != null && rt.poiPrefix != null && rt.isPOI()) {
			return rt.poiPrefix;
		}
		return null;
	}
	
	public AmenityType getAmenityType(String tag, String val, boolean hasName){
		return getAmenityType(tag, val, false, hasName);
	}
	
	public AmenityType getAmenityTypeForRelation(String tag, String val, boolean hasName){
		return getAmenityType(tag, val, true, hasName);
	}
	
	private AmenityType getAmenityType(String tag, String val, boolean relation, boolean hasName){
		// register amenity types
		Map<String, MapRulType> rules = getEncodingRuleTypes();
		MapRulType rt = rules.get(constructRuleKey(tag, val));
		if(rt != null && rt.isPOISpecified()) {
			if((relation && !rt.relation) || rt.isAdditionalOrText()) {
				return null;
			}
			if(rt.poiWithNameOnly && !hasName) {
				return null;
			}
			return rt.poiCategory;
		}
		rt = rules.get(constructRuleKey(tag, null));
		if(rt != null && rt.isPOISpecified()) {
			if((relation && !rt.relation) || rt.isAdditionalOrText()) {
				return null;
			}
			if(rt.poiWithNameOnly && !hasName) {
				return null;
			}
			return rt.poiCategory;
		}
		return null;
	}

	
	protected void init(){
		InputStream is;
		try {
			if(resourceName == null){
				is = MapRenderingTypes.class.getResourceAsStream("rendering_types.xml"); //$NON-NLS-1$
			} else {
				is = new FileInputStream(resourceName);
			}
			long time = System.currentTimeMillis();
			XmlPullParser parser = PlatformUtil.newXMLPullParser();
			int tok;
			parser.setInput(is, "UTF-8");
			MapRulType parentCategory = null;
			while ((tok = parser.next()) != XmlPullParser.END_DOCUMENT) {
				if (tok == XmlPullParser.START_TAG) {
					String name = parser.getName();
					if (name.equals("category")) { //$NON-NLS-1$
						parentCategory = parseCategoryFromXml(parser);
					} else if (name.equals("type")) {
						parseAndRegisterTypeFromXML(parser, parentCategory);
					} else if (name.equals("routing_type")) {
						parseRouteTagFromXML(parser);
					} else if (name.equals("entity_convert")) {
						parseEntityConvertXML(parser);
					}
				}
			}
			log.info("Time to init " + (System.currentTimeMillis() - time)); //$NON-NLS-1$
			is.close();
		} catch (IOException e) {
			log.error("Unexpected error", e); //$NON-NLS-1$
			e.printStackTrace();
			throw new RuntimeException(e);
		} catch (RuntimeException e) {
			log.error("Unexpected error", e); //$NON-NLS-1$
			e.printStackTrace();
			throw e;
		} catch (XmlPullParserException e) {
			log.error("Unexpected error", e); //$NON-NLS-1$
			e.printStackTrace();
			throw new RuntimeException(e);
		}
	}

	protected void parseEntityConvertXML(XmlPullParser parser) {
		
	}

	protected void parseRouteTagFromXML(XmlPullParser parser) {
	}

	protected void parseAndRegisterTypeFromXML(XmlPullParser parser, MapRulType parentCategory) {
		parseBaseRuleType(parser, parentCategory, true);
	}
	
	protected MapRulType parseBaseRuleType(XmlPullParser parser, MapRulType parentCategory, boolean filterOnlyMap) {
		String tag = lc(parser.getAttributeValue("", "tag"));
		return parseBaseRuleType(parser, parentCategory, filterOnlyMap, tag);
	}

	protected MapRulType parseBaseRuleType(XmlPullParser parser, MapRulType parentCategory, boolean filterOnlyMap,
			String tag) {
		String value = lc(parser.getAttributeValue("", "value"));
		String additional = parser.getAttributeValue("", "additional");
		if (value != null && value.length() == 0) { //$NON-NLS-1$
			value = null;
		}
		MapRulType rtype = MapRulType.createMainEntity(tag, value);
		if("true".equals(additional)) {
			rtype = MapRulType.createAdditional(tag, value);
		} else if("text".equals(additional)) {
			rtype = MapRulType.createText(tag);
		} 
		rtype.onlyMap  = "true".equals(parser.getAttributeValue("", "only_map"));
		if(filterOnlyMap && rtype.onlyMap) {
			return null;
		}
		
		String order = parser.getAttributeValue("", "order");
		if(!Algorithms.isEmpty(order)) {
			rtype.order = Integer.parseInt(order);
		} else if (parentCategory != null) {
			rtype.order = parentCategory.order;
		}
		if(!rtype.onlyMap) {
			rtype = registerRuleType(rtype);
		}

		rtype.category = parentCategory == null ? null : parentCategory.category;
		if (parentCategory != null) {
			if (parentCategory.poiCategory != null) {
				rtype.poiCategory = parentCategory.poiCategory;
				rtype.poiSpecified = true;
			}
			rtype.poiPrefix = parentCategory.poiPrefix;
			rtype.poiWithNameOnly = parentCategory.poiWithNameOnly;
		}

		String poiCategory = parser.getAttributeValue("", "poi_category");
		if (poiCategory != null) {
			rtype.poiSpecified = true;
			if (poiCategory.length() == 0) {
				rtype.poiCategory = null;
			} else {
				rtype.poiCategory = AmenityType.getAndRegisterType(poiCategory);
			}
		}
		String poiWithName = parser.getAttributeValue("", "poi_with_name");
		if (poiWithName != null) {
			rtype.poiWithNameOnly = Boolean.parseBoolean(poiWithName);
		}
		String poiPrefix = parser.getAttributeValue("", "poi_prefix");
		if (poiPrefix != null) {
			rtype.poiPrefix = poiPrefix;
		}
		
		rtype.onlyPoint = Boolean.parseBoolean(parser.getAttributeValue("", "point")); //$NON-NLS-1$
		rtype.relation = Boolean.parseBoolean(parser.getAttributeValue("", "relation")); //$NON-NLS-1$
		if (rtype.isMain()) {			
			rtype.namePrefix = parser.getAttributeValue("", "namePrefix"); //$NON-NLS-1$
			if (rtype.namePrefix == null) {
				rtype.namePrefix = "";
			}

			String v = parser.getAttributeValue("", "nameTags");
			if (v != null) {
				String[] names = v.split(",");
				rtype.names = new MapRulType[names.length];
				for (int i = 0; i < names.length; i++) {
					String tagName = names[i];
					if (rtype.namePrefix.length() > 0) {
						tagName = rtype.namePrefix + tagName;
					}
					MapRulType mt = MapRulType.createText(tagName);
					mt = registerRuleType(mt);
					rtype.names[i] = mt;
				}
			}
		}
		return rtype;
		
	}

	protected MapRulType registerRuleType(MapRulType rt) {
		String tag = rt.tagValuePattern.tag;
		String val = rt.tagValuePattern.value;
		String keyVal = constructRuleKey(tag, val);
		if(types.containsKey(keyVal)){
			MapRulType mapRulType = types.get(keyVal);
			if(mapRulType.isAdditional() || mapRulType.isText() ) {
				rt.id = mapRulType.id;
				if(rt.isMain()) {
					mapRulType.main = true;
					if(rt.minzoom != 0) {
						mapRulType.minzoom = Math.max(rt.minzoom, mapRulType.minzoom);
					}
					if(rt.maxzoom != 0) {
						mapRulType.maxzoom = Math.min(rt.maxzoom, mapRulType.maxzoom);
					}
				}
//				types.put(keyVal, rt);
//				typeList.set(rt.id, rt);
				return mapRulType;
			} else {
				throw new RuntimeException("Duplicate " + keyVal);
			}
		} else {
			rt.id = types.size();
			types.put(keyVal, rt);
			typeList.add(rt);
			return rt;
		}
	}

	protected MapRulType parseCategoryFromXml(XmlPullParser parser) {
		String poiTag = parser.getAttributeValue("", "poi_tag");
		String poiCategory = parser.getAttributeValue("", "poi_category");
		MapRulType rtype = new MapRulType();
		rtype.category = parser.getAttributeValue("", "name");
		if (!Algorithms.isEmpty(parser.getAttributeValue("", "order"))) {
			rtype.order = Integer.parseInt(parser.getAttributeValue("", "order"));
		}
		if (poiCategory != null && poiCategory.length() > 0) {
			rtype.poiCategory = AmenityType.getAndRegisterType(parser.getAttributeValue("", "poi_category"));
			rtype.poiSpecified = true;
			rtype.relation = Boolean.parseBoolean(parser.getAttributeValue("", "relation"));
			rtype.poiWithNameOnly = Boolean.parseBoolean(parser.getAttributeValue("", "poi_with_name"));
			rtype.poiPrefix = parser.getAttributeValue("", "poi_prefix");
			rtype.onlyPoi = true;
			if (poiTag != null) {
				rtype.tagValuePattern = new TagValuePattern(poiTag, null);
				return registerRuleType(rtype);
			}
		}
		return rtype;
	}
	
	protected static String constructRuleKey(String tag, String val) {
		if(val == null || val.length() == 0){
			return tag;
		}
		return tag + TAG_DELIMETER + val;
	}
	
	protected static String getTagKey(String tagValue) {
		int i = tagValue.indexOf(TAG_DELIMETER);
		if(i >= 0){
			return tagValue.substring(0, i);
		}
		return tagValue;
	}
	
	protected static String getValueKey(String tagValue) {
		int i = tagValue.indexOf(TAG_DELIMETER);
		if(i >= 0){
			return tagValue.substring(i + 1);
		}
		return null;
	}
	
		
	protected static class TagValuePattern {
		protected String tag;
		protected String value;
		protected TagValuePattern(String t, String v) {
			this.tag = t;
			this.value = v;
			if(tag == null && value == null) {
				throw new IllegalStateException("Tag/value null should be handled differently");
			}
			if(tag == null) {
				throw new UnsupportedOperationException();
			}
		}
		
		public boolean isApplicable(Map<String, String> e ){
			if(value == null) {
				return e.get(tag) != null;
			}
			return value.equals(e.get(tag));
		}
		
		@Override
		public String toString() {
			return "tag="+tag + " val=" +value;
		}
		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((tag == null) ? 0 : tag.hashCode());
			result = prime * result + ((value == null) ? 0 : value.hashCode());
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
			TagValuePattern other = (TagValuePattern) obj;
			if (tag == null) {
				if (other.tag != null)
					return false;
			} else if (!tag.equals(other.tag))
				return false;
			if (value == null) {
				if (other.value != null)
					return false;
			} else if (!value.equals(other.value))
				return false;
			return true;
		}
		
	}
	
	public static class MapRulType {
		protected MapRulType[] names;
		protected TagValuePattern tagValuePattern;
		protected boolean additional;
		protected boolean additionalText;
		protected boolean main;
		protected int order = 50;
		
		protected String category = null;
		protected String poiPrefix;
		protected boolean poiWithNameOnly;
		protected AmenityType poiCategory;
		// poi_category was specially removed for one tag/value, to skip unnecessary objects
		protected boolean poiSpecified;
		
		protected boolean relation;
		// creation of only section
		protected boolean onlyMap;
		protected boolean onlyPoi;
		
		// Needed only for map rules
		protected int minzoom;
		protected int maxzoom;
		protected boolean onlyPoint;
		protected String namePrefix ="";
		
		
		// inner id
		protected int id = -1;
		protected int freq;
		protected int targetId ;
		protected int targetPoiId = -1;
		
		private MapRulType(){
		}
		
		public boolean isPOI(){
			return !onlyMap;
		}
		
		public boolean isPOISpecified() {
			return isPOI() && poiSpecified;
		}
		
		public boolean isMap(){
			return !onlyPoi;
		}
		
		public int getOrder() {
			return order;
		}
		
		public static MapRulType createMainEntity(String tag, String value) {
			MapRulType rt = new MapRulType();
			rt.tagValuePattern = new TagValuePattern(tag, value);
			rt.main = true;
			return rt;
		}
		
		public static MapRulType createText(String tag) {
			MapRulType rt = new MapRulType();
			rt.additionalText = true;
			rt.minzoom = 2;
			rt.maxzoom = 31;
			rt.tagValuePattern = new TagValuePattern(tag, null); 
			return rt;
		}
		
		public static MapRulType createAdditional(String tag, String value) {
			MapRulType rt = new MapRulType();
			rt.additional = true;
			rt.minzoom = 2;
			rt.maxzoom = 31;
			rt.tagValuePattern = new TagValuePattern(tag, value);
			return rt;
		}


		public String getTag() {
			return tagValuePattern.tag;
		}
		
		public int getTargetId() {
			return targetId;
		}
		
		public int getTargetPoiId() {
			return targetPoiId;
		}
		
		public void setTargetPoiId(int catId, int valueId) {
			if(catId <= 31) {
				this.targetPoiId  = (valueId << 6) | (catId << 1) ; 
			} else {
				if(catId > (1 << 15)) {
					throw new IllegalArgumentException("Refer source code");
				}
				this.targetPoiId  = (valueId << 16) | (catId << 1) | 1;
			}
		}
		
		public int getInternalId() {
			return id;
		}
		
		public void setTargetId(int targetId) {
			this.targetId = targetId;
		}
		
		public String getValue() {
			return tagValuePattern.value;
		}
		
		public int getMinzoom() {
			return minzoom;
		}
		
		public boolean isAdditional() {
			return additional;
		}
		
		public boolean isAdditionalOrText() {
			return additional || additionalText;
		}
		
		public boolean isMain() {
			return main;
		}
		
		public boolean isText() {
			return additionalText;
		}
		
		public boolean isOnlyPoint() {
			return onlyPoint;
		}
		
		public boolean isRelation() {
			return relation;
		}
		
		public int getFreq() {
			return freq;
		}
		
		public int updateFreq(){
			return ++freq;
		}
		
		@Override
		public String toString() {
			return getTag() + " " + getValue();
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + id;
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
			MapRulType other = (MapRulType) obj;
			if (id != other.id || id < 0)
				return false;
			return true;
		}
		
		
	}

	
}

