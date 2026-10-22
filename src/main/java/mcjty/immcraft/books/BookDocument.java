package mcjty.immcraft.books;

import com.google.gson.*;
import net.minecraft.network.chat.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.DyeColor;
import java.awt.Font;
import java.awt.font.FontRenderContext;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/** Original # markup, section pagination, rich images/items and hyperlinks in logical 768x900 units. */
public final class BookDocument {
    public static final int WIDTH=768,HEIGHT=900,MARGIN=40;
    public enum Type { TEXT, ITEM, IMAGE, RULE }
    public record Format(float scale,int color,int align,int valign,boolean bold,boolean italic) {
        public static Format parse(String value) {
            float scale=1;int color=0x1d1d21,align=-1,valign=-1;boolean bold=false,italic=false;
            for(String s:value.split(",")) {
                if(s.matches("[0-9]")) scale=.5f+Integer.parseInt(s)*.4f;
                else switch(s) {
                    case "center","c" -> align=0;
                    case "right","r" -> align=1;
                    case "left","l" -> align=-1;
                    case "top","t" -> valign=-1;
                    case "vcenter","vc" -> valign=0;
                    case "bottom","b" -> valign=1;
                    case "bold" -> bold=true;
                    case "italic" -> italic=true;
                    default -> { for(DyeColor dye:DyeColor.values()) if(dye.getName().equals(s)) color=dye.getTextColor(); }
                }
            }
            return new Format(scale,color,align,valign,bold,italic);
        }
        public ResourceLocation font() { return new ResourceLocation("immcraft",bold?"ubuntu_bold":italic?"ubuntu_italic":"ubuntu"); }
        public Component component(String text) { return Component.literal(text).setStyle(Style.EMPTY.withFont(font()).withColor(color)); }
    }
    public record Element(Type type,String value,Format format,int x,int y,int width,int height,String link,int u,int v,int tw,int th) {
        public boolean contains(double px,double py) { return px>=x && py>=y && px<x+width && py<y+height; }
    }
    public record Page(List<Element> elements,Set<String> sections) {
        public String linkAt(double x,double y) {
            for(Element e:elements) if(e.link!=null && e.contains(x,y)) return e.link;
            return null;
        }
    }
    private final List<Page> pages;
    private static final Map<ResourceLocation,BookDocument> BUILTIN=new ConcurrentHashMap<>();
    private static final FontRenderContext FRC=new FontRenderContext(null,true,true);
    private static final Font[] FONTS=fonts();
    private static Font[] fonts() {
        Font[] fonts=new Font[3];String[] names={"ubuntu","ubuntu_bold","ubuntu_italic"};
        for(int i=0;i<3;i++) try(InputStream in=BookDocument.class.getResourceAsStream("/assets/immcraft/font/"+names[i]+".ttf")) {
            if(in==null) throw new IOException("Missing original Ubuntu font");
            fonts[i]=Font.createFont(Font.TRUETYPE_FONT,in).deriveFont(64f);
        } catch(Exception e) { throw new IllegalStateException("Cannot load book font "+names[i],e); }
        return fonts;
    }
    public static BookDocument builtin(ResourceLocation id) {
        return BUILTIN.computeIfAbsent(id,key->{
            try(InputStream in=BookDocument.class.getResourceAsStream("/assets/"+id.getNamespace()+"/"+id.getPath())) {
                if(in==null) throw new IOException("Book not found: "+id);
                return parse(new InputStreamReader(in,StandardCharsets.UTF_8));
            } catch(IOException e) { throw new IllegalArgumentException("Cannot read book "+id,e); }
        });
    }
    public static int textWidth(String text,Format fmt) { return (int)Math.ceil(FONTS[fmt.bold?1:fmt.italic?2:0].getStringBounds(text,FRC).getWidth()*fmt.scale); }
    public BookDocument(List<Page> pages) { this.pages=List.copyOf(pages); }
    public List<Page> pages() { return pages; }
    public int clamp(int n) { return Math.max(0,Math.min(pages.size()-1,n)); }
    public int navigate(int page,String action) {
        if(action==null) return clamp(page);
        if(action.equals("<")) return clamp(page-1);
        if(action.equals(">")) return clamp(page+1);
        if(action.equals("^")) return 0;
        for(int i=0;i<pages.size();i++) if(pages.get(i).sections.contains(action)) return i;
        return clamp(page);
    }
    private static final class Layout {
        final List<Element> elements=new ArrayList<>();int x,y,lineHeight;
        void newline() { y+=lineHeight;lineHeight=0;x=0; }
        void space() { if(x>0) x+=16; }
        void add(Type type,String value,Format fmt,int width,int height,String link,int u,int v,int tw,int th) {
            if(x+width>WIDTH && x>0) newline();
            int left=fmt.align==0?(WIDTH-width)/2:fmt.align==1?WIDTH-width:x;
            elements.add(new Element(type,value,fmt,left,y,width,height,link,u,v,tw,th));
            x+=width;lineHeight=Math.max(lineHeight,height);
        }
        void text(String text,Format fmt,String link) {
            if(fmt.align!=-1 || link!=null) { add(Type.TEXT,text,fmt,textWidth(text,fmt),(int)(64*fmt.scale),link,0,0,0,0);return; }
            for(String word:text.trim().split("\\s+")) if(!word.isEmpty()) {
                space();add(Type.TEXT,word,fmt,textWidth(word,fmt),(int)(64*fmt.scale),null,0,0,0,0);
            }
        }
    }
    public static BookDocument parse(Reader reader) {
        JsonArray root=JsonParser.parseReader(reader).getAsJsonArray();
        List<Page> pages=new ArrayList<>();List<Element> page=new ArrayList<>();Set<String> sections=new LinkedHashSet<>();int pageY=0;
        for(JsonElement entry:root) {
            JsonObject object=entry.getAsJsonObject();Layout l=new Layout();
            if(object.has("text")) for(JsonElement value:object.getAsJsonArray("text")) {
                if(!value.isJsonPrimitive() || !value.getAsJsonPrimitive().isString()) continue;
                String s=value.getAsString();
                if(s.equals("#") || s.equals("#>")) { l.newline();if(s.equals("#>")) l.x=40; }
                else if(s.equals("##")) { if(l.lineHeight==0) l.lineHeight=64;l.newline(); }
                else if(s.equals("#-")) { l.add(Type.RULE,"",Format.parse(""),WIDTH,8,null,0,0,0,0);l.newline(); }
                else if(s.startsWith("#:")) {
                    String rest=s.substring(2);int split=rest.indexOf(':');
                    l.text(split<0?rest:rest.substring(split+1),Format.parse(split<0?"":rest.substring(0,split)),null);
                } else if(s.startsWith("#l")) {
                    int colon=s.indexOf(':',2);if(colon<0) continue;
                    String target=s.substring(colon+1);Format f=Format.parse(s.substring(2,colon)+",blue");l.space();l.text(target,f,target);
                } else if(s.startsWith("#i") || s.startsWith("#I")) {
                    int colon=s.indexOf(':',2);if(colon<0) continue;
                    boolean image=s.charAt(1)=='I';float scale=1;
                    if(colon>2) try { scale=.5f+Integer.parseInt(s.substring(2,colon))*(image?.6f:.4f); } catch(NumberFormatException ignored) {}
                    String resource=s.substring(colon+1);
                    if(image) {
                        int last=resource.lastIndexOf(':');if(last<=resource.indexOf(':')) continue;
                        String[] crop=resource.substring(last+1).split(",");resource=resource.substring(0,last);
                        int u=number(crop,0,0),v=number(crop,1,0),w=number(crop,2,16),h=number(crop,3,16),tw=number(crop,4,256),th=number(crop,5,256);
                        l.add(Type.IMAGE,resource,Format.parse(""),Math.max(1,(int)(w*scale)),Math.max(1,(int)(h*scale)),null,u,v,tw,th);
                        // Store source crop independently of its scaled destination.
                        Element e=l.elements.remove(l.elements.size()-1);
                        l.elements.add(new Element(e.type,e.value,e.format,e.x,e.y,e.width,e.height,w+","+h,e.u,e.v,e.tw,e.th));
                    } else l.add(Type.ITEM,modernItem(resource),Format.parse(""),Math.max(1,(int)(64*scale)),Math.max(1,(int)(64*scale)),null,0,0,0,0);
                } else l.text(s,Format.parse(""),null);
            }
            l.newline();int sectionHeight=l.y;
            if(sectionHeight>HEIGHT) {
                // Preserve the original per-section no-fit diagnostic instead of silently truncating text.
                l=new Layout();l.text("<NO FIT: "+sectionHeight+"/"+HEIGHT+">",Format.parse("red"),null);l.newline();sectionHeight=l.y;
            }
            if(pageY+sectionHeight>HEIGHT && !page.isEmpty()) { pages.add(new Page(List.copyOf(page),Set.copyOf(sections)));page.clear();sections.clear();pageY=0; }
            if(object.has("section")) sections.add(object.get("section").getAsString());
            for(Element e:l.elements) page.add(new Element(e.type,e.value,e.format,e.x,e.y+pageY,e.width,e.height,e.link,e.u,e.v,e.tw,e.th));
            pageY+=sectionHeight+MARGIN;
            if(object.has("page")) { pages.add(new Page(List.copyOf(page),Set.copyOf(sections)));page.clear();sections.clear();pageY=0; }
        }
        if(!page.isEmpty() || pages.isEmpty()) pages.add(new Page(List.copyOf(page),Set.copyOf(sections)));
        return new BookDocument(pages);
    }
    private static int number(String[] data,int n,int fallback) { try { return Integer.parseInt(data[n]); } catch(Exception e) { return fallback; } }
    private static String modernItem(String old) {
        return switch(old) { case "minecraft:planks","minecraft:planks@0" -> "minecraft:oak_planks";case "minecraft:log","minecraft:log@0" -> "minecraft:oak_log";default -> old; };
    }
}
