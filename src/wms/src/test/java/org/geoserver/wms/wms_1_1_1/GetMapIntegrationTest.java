/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */
package org.geoserver.wms.wms_1_1_1;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.awt.Color;
import java.awt.Transparency;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.IndexColorModel;
import java.awt.image.RenderedImage;
import java.io.File;
import java.io.InputStream;
import java.io.Serializable;
import java.net.URL;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.imageio.ImageIO;
import javax.servlet.ServletResponse;
import javax.xml.namespace.QName;

import org.apache.commons.io.FileUtils;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CoverageInfo;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.data.test.MockData;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.data.test.SystemTestData.LayerProperty;
import org.geoserver.test.RemoteOWSTestSupport;
import org.geoserver.wms.WMSTestSupport;
import org.geotools.gce.imagemosaic.ImageMosaicFormat;
import org.junit.Test;
import org.w3c.dom.Document;

import com.mockrunner.mock.web.MockHttpServletResponse;

public class GetMapIntegrationTest extends WMSTestSupport {
    String bbox = "-130,24,-66,50";

    String styles = "states";

    String layers = "sf:states";

    public static final String STATES_SLD = "<StyledLayerDescriptor version=\"1.0.0\">"
            + "<UserLayer><Name>sf:states</Name><UserStyle><Name>UserSelection</Name>"
            + "<FeatureTypeStyle><Rule><Filter xmlns:gml=\"http://www.opengis.net/gml\">"
            + "<PropertyIsEqualTo><PropertyName>STATE_ABBR</PropertyName><Literal>IL</Literal></PropertyIsEqualTo>"
            + "</Filter><PolygonSymbolizer><Fill><CssParameter name=\"fill\">#FF0000</CssParameter></Fill>"
            + "</PolygonSymbolizer></Rule><Rule><LineSymbolizer><Stroke/></LineSymbolizer></Rule>"
            + "</FeatureTypeStyle></UserStyle></UserLayer></StyledLayerDescriptor>";
    
    public static final String STATES_SLD11 = 
        "<StyledLayerDescriptor version=\"1.1.0\"> "+
        " <UserLayer> "+
        "  <Name>sf:states</Name> "+
        "  <UserStyle> "+
        "   <Name>UserSelection</Name> "+
        "   <se:FeatureTypeStyle xmlns:se=\"http://www.opengis.net/se\"> "+
        "    <se:Rule> "+
        "     <ogc:Filter xmlns:ogc=\"http://www.opengis.net/ogc\"> "+
        "      <ogc:PropertyIsEqualTo> "+
        "       <ogc:PropertyName>STATE_ABBR</ogc:PropertyName> "+
        "       <ogc:Literal>IL</ogc:Literal> "+
        "      </ogc:PropertyIsEqualTo> "+
        "     </ogc:Filter> "+
        "     <se:PolygonSymbolizer> "+
        "      <se:Fill> "+
        "       <se:SvgParameter name=\"fill\">#FF0000</se:SvgParameter> "+
        "      </se:Fill> "+
        "     </se:PolygonSymbolizer> "+
        "    </se:Rule> "+
        "    <se:Rule> "+
        "     <se:LineSymbolizer> "+
        "      <se:Stroke/> "+
        "     </se:LineSymbolizer> "+
        "    </se:Rule> "+
        "   </se:FeatureTypeStyle> "+
        "  </UserStyle> "+
        " </UserLayer> "+
        "</StyledLayerDescriptor>";


    public static final String STATES_GETMAP = //
    "<?xml version=\"1.0\" encoding=\"UTF-8\"?> \n "
            + "<ogc:GetMap service=\"WMS\"  version=\"1.1.1\" \n "
            + "        xmlns:gml=\"http://www.opengis.net/gml\"\n "
            + "        xmlns:ogc=\"http://www.opengis.net/ows\"\n "
            + "        xmlns:sld=\"http://www.opengis.net/sld\"\n "
            + "        xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n "
            + "        xsi:schemaLocation=\"http://www.opengis.net/ows GetMap.xsd http://www.opengis.net/gml geometry.xsd http://www.opengis.net/sld StyledLayerDescriptor.xsd \">\n "
            + "        <sld:StyledLayerDescriptor>\n " + "                <sld:NamedLayer>\n "
            + "                        <sld:Name>sf:states</sld:Name>\n "
            + "                        <sld:NamedStyle>\n "
            + "                                <sld:Name>Default</sld:Name>\n "
            + "                        </sld:NamedStyle>\n "
            + "                </sld:NamedLayer>\n " + "        </sld:StyledLayerDescriptor>\n "
            + "        <ogc:BoundingBox srsName=\"4326\">\n " + "                <gml:coord>\n "
            + "                        <gml:X>-130</gml:X>\n "
            + "                        <gml:Y>24</gml:Y>\n " + "                </gml:coord>\n "
            + "                <gml:coord>\n " + "                        <gml:X>-66</gml:X>\n "
            + "                        <gml:Y>50</gml:Y>\n " + "                </gml:coord>\n "
            + "        </ogc:BoundingBox>\n " + "        <ogc:Output>\n "
            + "                <ogc:Format>image/png</ogc:Format>\n "
            + "                <ogc:Size>\n "
            + "                        <ogc:Width>550</ogc:Width>\n "
            + "                        <ogc:Height>250</ogc:Height>\n "
            + "                </ogc:Size>\n " + "        </ogc:Output>\n " + "</ogc:GetMap>\n ";

  
    @Override
    protected void setUpTestData(SystemTestData testData) throws Exception {
        super.setUpTestData(testData);
        testData.setUpWcs11RasterLayers();
    }
    
    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        super.onSetUp(testData);
        Catalog catalog = getCatalog();
        testData.addStyle("Population","Population.sld",GetMapIntegrationTest.class,catalog);
        testData.addVectorLayer(new QName(MockData.SF_URI, "states", MockData.SF_PREFIX),
                Collections.EMPTY_MAP,"states.properties", getClass(),catalog);
        // add a parametric style to the mix
        testData.addStyle("parametric", "parametric.sld",org.geoserver.wms.map.GetMapIntegrationTest.class,catalog);
        
        // add a translucent style to the mix
        testData.addStyle("translucent", "translucent.sld",GetMapIntegrationTest.class,catalog);

        testData.addStyle("raster", "raster.sld",SystemTestData.class,catalog);
        testData.addStyle("demTranslucent","demTranslucent.sld",GetMapIntegrationTest.class,catalog);
        
        Map properties = new HashMap();
        properties.put(LayerProperty.STYLE,"raster");
        testData.addRasterLayer(new QName(MockData.SF_URI, "mosaic_holes", MockData.SF_PREFIX),
                "mosaic_holes.zip", null, properties,GetMapIntegrationTest.class,catalog);
    }
    
    // protected String getDefaultLogConfiguration() {
    // return "/DEFAULT_LOGGING.properties";
    // }

    @Test
    public void testImage() throws Exception {
        MockHttpServletResponse response = getAsServletResponse("wms?bbox=" + bbox
                + "&styles=&layers=" + layers + "&Format=image/png" + "&request=GetMap"
                + "&width=550" + "&height=250" + "&srs=EPSG:4326");
        checkImage(response);
    }
    
    @Test
    public void testLayoutLegendNPE() throws Exception {
        // set the title to null
        FeatureTypeInfo states = getCatalog().getFeatureTypeByName("states");
        states.setTitle(null);
        getCatalog().save(states);
        
        // add the layout to the data dir
        File layouts = getDataDirectory().findOrCreateDir("layouts");
        URL layout = GetMapIntegrationTest.class.getResource("test-layout.xml");
        FileUtils.copyURLToFile(layout, new File(layouts, "test-layout.xml"));
        
        // get a map with the layout, it used to NPE
        BufferedImage image = getAsImage("wms?bbox=" + bbox
                + "&styles=Population&layers=" + layers + "&Format=image/png" + "&request=GetMap"
                + "&width=550" + "&height=250" + "&srs=EPSG:4326&format_options=layout:test-layout", "image/png");
        // RenderedImageBrowser.showChain(image);

        // check the pixels that should be in the legend
        assertPixel(image, 12, 16, Color.RED);
        assertPixel(image, 12, 32, Color.GREEN);
        assertPixel(image, 12, 52, Color.BLUE);
    }
    
    @Test
    public void testLayoutLegendStyleTitle() throws Exception {
        // set the title to null
        FeatureTypeInfo states = getCatalog().getFeatureTypeByName("states");
        states.setTitle(null);
        getCatalog().save(states);
        
        // add the layout to the data dir
        File layouts = getDataDirectory().findOrCreateDir("layouts");
        URL layout = GetMapIntegrationTest.class.getResource("test-layout-sldtitle.xml");
        FileUtils.copyURLToFile(layout, new File(layouts, "test-layout-sldtitle.xml"));
        
        // get a map with the layout, it used to NPE
        BufferedImage image = getAsImage("wms?bbox=" + bbox
                + "&styles=Population&layers=" + layers + "&Format=image/png" + "&request=GetMap"
                + "&width=550" + "&height=250" + "&srs=EPSG:4326&format_options=layout:test-layout-sldtitle", "image/png");
        // RenderedImageBrowser.showChain(image);

        // check the pixels that should be in the legend
        assertPixel(image, 12, 36, Color.RED);
        assertPixel(image, 12, 52, Color.GREEN);
        assertPixel(image, 12, 72, Color.BLUE);
    }
    
    @Test
    public void testLayoutTranslucent() throws Exception {
        // add the layout to the data dir
        File layouts = getDataDirectory().findOrCreateDir("layouts");
        URL layout = GetMapIntegrationTest.class.getResource("test-layout.xml");
        FileUtils.copyURLToFile(layout, new File(layouts, "test-layout.xml"));
        
        // get a map with the layout after using a translucent style
        BufferedImage image = getAsImage("wms?bbox=" + bbox
                + "&styles=translucent&layers=" + layers + "&Format=image/png" + "&request=GetMap"
                + "&width=550" + "&height=250" + "&srs=EPSG:4326" 
                + "&format_options=layout:test-layout&transparent=true", "image/png");
        // RenderedImageBrowser.showChain(image);
        
        // check the pixels that should be in the scale bar
        assertPixel(image, 56, 211, Color.WHITE);
        assertPixel(image, 52, 221, Color.BLACK);
    }
    
    @Test
    public void testGeotiffMime() throws Exception {
        MockHttpServletResponse response = getAsServletResponse("wms?bbox=" + bbox
                + "&styles=&layers=" + layers + "&Format=image/geotiff" + "&request=GetMap"
                + "&width=550" + "&height=250" + "&srs=EPSG:4326");
        assertEquals("image/geotiff", response.getContentType());
        assertEquals("inline; filename=sf-states.tif", response.getHeader("Content-Disposition"));
    }
    
    @Test 
    public void testPng8Opaque() throws Exception {
        MockHttpServletResponse response = getAsServletResponse("wms?bbox=" + bbox
                + "&styles=&layers=" + layers + "&Format=image/png8" + "&request=GetMap"
                + "&width=550" + "&height=250" + "&srs=EPSG:4326");
        assertEquals("image/png; mode=8bit", response.getContentType());
        assertEquals("inline; filename=sf-states.png", response.getHeader("Content-Disposition"));
        
        InputStream is = getBinaryInputStream(response);
        BufferedImage bi = ImageIO.read(is);
        IndexColorModel cm = (IndexColorModel) bi.getColorModel();
        assertEquals(Transparency.OPAQUE , cm.getTransparency());
        assertEquals(-1, cm.getTransparentPixel());
    }
    
    @Test
    public void testPng8ForceBitmask() throws Exception {
        MockHttpServletResponse response = getAsServletResponse("wms?bbox=" + bbox
                + "&styles=&layers=" + layers + "&Format=image/png8" + "&request=GetMap"
                + "&width=550" + "&height=250" + "&srs=EPSG:4326&transparent=true&format_options=quantizer:octree");
        assertEquals("image/png; mode=8bit", response.getContentType());
        assertEquals("inline; filename=sf-states.png", response.getHeader("Content-Disposition"));
        
        InputStream is = getBinaryInputStream(response);
        BufferedImage bi = ImageIO.read(is);
        IndexColorModel cm = (IndexColorModel) bi.getColorModel();
        assertEquals(Transparency.BITMASK , cm.getTransparency());
        assertTrue(cm.getTransparentPixel() >= 0);
    }
    
    @Test 
    public void testPng8Translucent() throws Exception {
        MockHttpServletResponse response = getAsServletResponse("wms?bbox=" + bbox
                + "&styles=&layers=" + layers + "&Format=image/png8" + "&request=GetMap"
                + "&width=550" + "&height=250" + "&srs=EPSG:4326&transparent=true");
        assertEquals("image/png; mode=8bit", response.getContentType());
        assertEquals("inline; filename=sf-states.png", response.getHeader("Content-Disposition"));
        
        InputStream is = getBinaryInputStream(response);
        BufferedImage bi = ImageIO.read(is);
        IndexColorModel cm = (IndexColorModel) bi.getColorModel();
        assertEquals(Transparency.TRANSLUCENT , cm.getTransparency());
    }

    
    @Test 
    public void testDefaultContentDisposition() throws Exception {
        MockHttpServletResponse response = getAsServletResponse("wms?bbox=" + bbox
                + "&styles=&layers=" + layers + "&Format=image/png" + "&request=GetMap"
                + "&width=550" + "&height=250" + "&srs=EPSG:4326");
        assertEquals("image/png", response.getContentType());
        assertEquals("inline; filename=sf-states.png", response.getHeader("Content-Disposition"));
    }
    
    @Test
    public void testForcedContentDisposition() throws Exception {
        MockHttpServletResponse response = getAsServletResponse("wms?bbox=" + bbox
                + "&styles=&layers=" + layers + "&Format=image/png" + "&request=GetMap"
                + "&width=550" + "&height=250" + "&srs=EPSG:4326&content-disposition=attachment");
        assertEquals("image/png", response.getContentType());
        assertEquals("attachment; filename=sf-states.png", response.getHeader("Content-Disposition"));
    }
    
    @Test
    public void testForcedFilename() throws Exception {
        MockHttpServletResponse response = getAsServletResponse("wms?bbox=" + bbox
                + "&styles=&layers=" + layers + "&Format=image/png" + "&request=GetMap"
                + "&width=550" + "&height=250" + "&srs=EPSG:4326&filename=dude.png");
        assertEquals("image/png", response.getContentType());
        assertEquals("inline; filename=dude.png", response.getHeader("Content-Disposition"));
    }
    
    @Test
    public void testForcedContentDispositionFilename() throws Exception {
        MockHttpServletResponse response = getAsServletResponse("wms?bbox=" + bbox
                + "&styles=&layers=" + layers + "&Format=image/png" + "&request=GetMap"
                + "&width=550" + "&height=250" + "&srs=EPSG:4326&content-disposition=attachment&filename=dude.png");
        assertEquals("image/png", response.getContentType());
        assertEquals("attachment; filename=dude.png", response.getHeader("Content-Disposition"));
    }
    

    @Test
    public void testSldBody() throws Exception {
        MockHttpServletResponse response = getAsServletResponse("wms?bbox=" + bbox + "&styles="
                + "&layers=" + layers + "&Format=image/png" + "&request=GetMap" + "&width=550"
                + "&height=250" + "&srs=EPSG:4326" + "&SLD_BODY="
                + STATES_SLD.replaceAll("=", "%3D"));
        checkImage(response);
    }
    
    @Test 
    public void testSldBody11() throws Exception {
        MockHttpServletResponse response = getAsServletResponse("wms?bbox=" + bbox + "&styles="
                + "&layers=" + layers + "&Format=image/png" + "&request=GetMap" + "&width=550"
                + "&height=250" + "&srs=EPSG:4326" + "&SLD_BODY="
                + STATES_SLD11.replaceAll("=", "%3D"));
        checkImage(response);
    }
    
    @Test
    public void testSldBodyNoVersion() throws Exception {
        MockHttpServletResponse response = getAsServletResponse("wms?bbox=" + bbox + "&styles="
                + "&layers=" + layers + "&Format=image/png" + "&request=GetMap" + "&width=550"
                + "&height=250" + "&srs=EPSG:4326" + "&SLD_BODY="
                + STATES_SLD.replace(" version=\"1.1.0\"", "").replaceAll("=", "%3D"));
        checkImage(response);
    }

    @Test 
    public void testSldBodyPost() throws Exception {
        MockHttpServletResponse response = postAsServletResponse("wms?bbox=" + bbox
                + "&format=image/png&request=GetMap&width=550&height=250" + "&srs=EPSG:4326",
                STATES_SLD);

        checkImage(response);
    }
    
    @Test
    public void testSldBodyPost11() throws Exception {
        MockHttpServletResponse response = postAsServletResponse("wms?bbox=" + bbox
                + "&format=image/png&request=GetMap&width=550&height=250" + "&srs=EPSG:4326",
                STATES_SLD11);

        checkImage(response);
    }

    @Test
    public void testXmlPost() throws Exception {
        MockHttpServletResponse response = postAsServletResponse("wms?", STATES_GETMAP);
        checkImage(response);
    }

    @Test
    public void testRemoteOWSGet() throws Exception {
        if (!RemoteOWSTestSupport.isRemoteWFSStatesAvailable(LOGGER))
            return;

        ServletResponse response = getAsServletResponse("wms?request=getmap&service=wms&version=1.1.1"
                + "&format=image/png"
                + "&layers="
                + RemoteOWSTestSupport.TOPP_STATES
                + ","
                + MockData.BASIC_POLYGONS.getPrefix()
                + ":"
                + MockData.BASIC_POLYGONS.getLocalPart()
                + "&styles=Population,"
                + MockData.BASIC_POLYGONS.getLocalPart()
                + "&remote_ows_type=WFS"
                + "&remote_ows_url="
                + RemoteOWSTestSupport.WFS_SERVER_URL
                + "&height=1024&width=1024&bbox=-180,-90,180,90&srs=EPSG:4326");

        assertEquals("image/png", response.getContentType());
    }

    @Test
    public void testRemoteOWSUserStyleGet() throws Exception {
        if (!RemoteOWSTestSupport.isRemoteWFSStatesAvailable(LOGGER)) {
            return;
        }

        URL url = GetMapIntegrationTest.class.getResource("remoteOws.sld");

        ServletResponse response = getAsServletResponse("wms?request=getmap&service=wms&version=1.1.1"
                + "&format=image/png"
                + "&sld="
                + url.toString()
                + "&height=1024&width=1024&bbox=-180,-90,180,90&srs=EPSG:4326");

        assertEquals("image/png", response.getContentType());
    }

    @Test 
    public void testWorkspaceQualified() throws Exception {

        Document doc = getAsDOM("cite/wms?request=getmap&service=wms"
                + "&layers=PrimitiveGeoFeature&width=100&height=100&format=image/png"
                + "&srs=epsg:4326&bbox=-180,-90,180,90", true);
        assertEquals("ServiceExceptionReport", doc.getDocumentElement().getNodeName());

        ServletResponse response = getAsServletResponse("cite/wms?request=getmap&service=wms"
                + "&layers=Lakes&width=100&height=100&format=image/png"
                + "&srs=epsg:4326&bbox=-180,-90,180,90");
        assertEquals("image/png", response.getContentType());
    }

    @Test
    public void testLayerQualified() throws Exception {
        Document doc = getAsDOM("cite/Ponds/wms?request=getmap&service=wms"
                + "&layers=Forests&width=100&height=100&format=image/png"
                + "&srs=epsg:4326&bbox=-180,-90,180,90", true);
        assertEquals("ServiceExceptionReport", doc.getDocumentElement().getNodeName());

        ServletResponse response = getAsServletResponse("cite/Ponds/wms?request=getmap&service=wms"
                + "&layers=Ponds&width=100&height=100&format=image/png"
                + "&srs=epsg:4326&bbox=-180,-90,180,90");
        assertEquals("image/png", response.getContentType());
    }
    
    @Test
    public void testGroupWorkspaceQualified() throws Exception {
        // check the group works without workspace qualification
        String url = "wms?request=getmap&service=wms"
                + "&layers=nature&width=100&height=100&format=image/png"
                + "&srs=epsg:4326&bbox=-0.002,-0.003,0.005,0.002";
        ServletResponse response = getAsServletResponse(url);
        assertEquals("image/png", response.getContentType());
        
        // see that it works also with workspace qualification
        response = getAsServletResponse("cite/" + url);
        assertEquals("image/png", response.getContentType());
    }
    
    @Test
    public void testEnvDefault() throws Exception {
        MockHttpServletResponse response = getAsServletResponse("wms?bbox=" + bbox
                + "&styles=parametric&layers=" + layers + "&Format=image/png" + "&request=GetMap"
                + "&width=550" + "&height=250" + "&srs=EPSG:4326");
        assertEquals("image/png", response.getContentType());
        
        RenderedImage image = ImageIO.read(getBinaryInputStream(response));

        int[] rgba = new int[3];
        // fully black pixel in the middle of the map
        image.getData().getPixel(250, 125, rgba);
        //assertEquals(0, rgba[0]);
        //assertEquals(0, rgba[1]);
        //assertEquals(0, rgba[2]);
    }
    
    @Test
    public void testEnvRed() throws Exception {
        MockHttpServletResponse response = getAsServletResponse("wms?bbox=" + bbox
                + "&styles=parametric&layers=" + layers + "&Format=image/png" + "&request=GetMap"
                + "&width=550" + "&height=250" + "&srs=EPSG:4326&env=color:0xFF0000");
        assertEquals("image/png", response.getContentType());
        
        RenderedImage image = ImageIO.read(getBinaryInputStream(response));

        int[] rgba = new int[3];
        // fully red pixel in the middle of the map
        image.getData().getPixel(250, 125, rgba);
        //assertEquals(255, rgba[0]);
        //assertEquals(0, rgba[1]);
        //assertEquals(0, rgba[2]);
    }
    
    @Test
    public void testMosaicHoles() throws Exception {
        String url = "wms?LAYERS=sf%3Amosaic_holes&FORMAT=image%2Fpng&SERVICE=WMS&VERSION=1.1.1" +
        		"&REQUEST=GetMap&STYLES=&SRS=EPSG%3A4326" +
        		"&BBOX=6.40284375,36.385494140625,12.189662109375,42.444494140625" +
        		"&WIDTH=489&HEIGHT=512&transparent=true";
        BufferedImage bi = getAsImage(url, "image/png");
        int[] pixel = new int[4];
        bi.getRaster().getPixel(0, 250, pixel);
        assertTrue(Arrays.equals(new int[] {0,0,0,255}, pixel));
        
        // now reconfigure the mosaic for transparency
        CoverageInfo ci = getCatalog().getCoverageByName("sf:mosaic_holes");
        Map<String, Serializable> params = ci.getParameters();
        params.put(ImageMosaicFormat.INPUT_TRANSPARENT_COLOR.getName().getCode(), "#000000");
        params.put(ImageMosaicFormat.OUTPUT_TRANSPARENT_COLOR.getName().getCode(), "#000000");
        getCatalog().save(ci);
        
        // this time that pixel should be transparent
        bi = getAsImage(url, "image/png");
        bi.getRaster().getPixel(0, 250, pixel);
        assertTrue(Arrays.equals(new int[] {255,255,255,0}, pixel));
    }
    
    @Test
    public void testTransparentPaletteOpaqueOutput() throws Exception {
        String url = "wms?LAYERS=" + getLayerId(MockData.TASMANIA_DEM) + "&styles=demTranslucent&"
                + "FORMAT=image%2Fpng&SERVICE=WMS&VERSION=1.1.1"
                + "&REQUEST=GetMap&SRS=EPSG%3A4326"
                + "&BBOX=145,-43,146,-41&WIDTH=100&HEIGHT=200&bgcolor=0xFF0000";
        BufferedImage bi = getAsImage(url, "image/png");
        
        ColorModel cm = bi.getColorModel();
        assertTrue(cm instanceof IndexColorModel);
        assertEquals(Transparency.OPAQUE, cm.getTransparency());
        
        // grab a pixel in the low left corner, should be red (BG color)
        int[] pixel = new int[1];
        bi.getRaster().getPixel(4, 196, pixel);
        int[] color = new int[3];
        cm.getComponents(pixel[0], color, 0);
        assertEquals(255, color[0]);
        assertEquals(0, color[1]);
        assertEquals(0, color[2]);
        
        // a pixel high enough to be solid, should be fully green
        bi.getRaster().getPixel(56, 49, pixel);
        cm.getComponents(pixel[0], color, 0);
        assertEquals(0, color[0]);
        assertEquals(255, color[1]);
        assertEquals(0, color[2]);
    }
    
    @Test
    public void testTransparentPaletteTransparentOutput() throws Exception {
        String url = "wms?LAYERS=" + getLayerId(MockData.TASMANIA_DEM) + "&styles=demTranslucent&"
                + "FORMAT=image%2Fpng&SERVICE=WMS&VERSION=1.1.1"
                + "&REQUEST=GetMap&SRS=EPSG%3A4326"
                + "&BBOX=145,-43,146,-41&WIDTH=100&HEIGHT=200&transparent=true";
        BufferedImage bi = getAsImage(url, "image/png");
        
        ColorModel cm = bi.getColorModel();
        assertTrue(cm instanceof IndexColorModel);
        assertEquals(Transparency.TRANSLUCENT, cm.getTransparency());
        
        // grab a pixel in the low left corner, should be transparent
        int[] pixel = new int[1];
        bi.getRaster().getPixel(4, 196, pixel);
        int[] color = new int[4];
        cm.getComponents(pixel[0], color, 0);
        assertEquals(0, color[3]);
        
        // a pixel high enough to be solid, should be solid green
        bi.getRaster().getPixel(56, 49, pixel);
        cm.getComponents(pixel[0], color, 0);
        assertEquals(0, color[0]);
        assertEquals(255, color[1]);
        assertEquals(0, color[2]);
        assertEquals(255, color[3]);
    }
    
    @Test 
    public void testTransparentPaletteTransparentOutputPng8() throws Exception {
        String url = "wms?LAYERS=" + getLayerId(MockData.TASMANIA_DEM) + "&styles=demTranslucent&"
                + "FORMAT=image%2Fpng8&SERVICE=WMS&VERSION=1.1.1"
                + "&REQUEST=GetMap&SRS=EPSG%3A4326"
                + "&BBOX=145,-43,146,-41&WIDTH=100&HEIGHT=200&transparent=true";
        BufferedImage bi = getAsImage(url, "image/png; mode=8bit");
        
        ColorModel cm = bi.getColorModel();
        assertTrue(cm instanceof IndexColorModel);
        assertEquals(Transparency.TRANSLUCENT, cm.getTransparency());
        
        // grab a pixel in the low left corner, should be transparent
        int[] pixel = new int[1];
        bi.getRaster().getPixel(4, 196, pixel);
        int[] color = new int[4];
        cm.getComponents(pixel[0], color, 0);
        assertEquals(0, color[3]);
        
        // a pixel high enough to be solid, should be solid green
        bi.getRaster().getPixel(56, 49, pixel);
        cm.getComponents(pixel[0], color, 0);
        assertEquals(0, color[0]);
        assertEquals(255, color[1]);
        assertEquals(0, color[2]);
        assertEquals(255, color[3]);
    }

}
