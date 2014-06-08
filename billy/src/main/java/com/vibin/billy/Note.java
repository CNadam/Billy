package com.vibin.billy;

import org.simpleframework.xml.Element;
import org.simpleframework.xml.ElementList;
import org.simpleframework.xml.ElementUnion;
import org.simpleframework.xml.Root;
import org.simpleframework.xml.Attribute;

import java.util.List;

@Root(name="rss") //root of the xml file
public class Note {
    @ElementList(name="channel")
    private List channel;

    @Root(name="item")
    public class item{
        @ElementUnion({
                @Element(name = "title", type = String.class),
                @Element(name = "link", type = String.class),
                @Element(name = "description", type = String.class)
        })
        private Object value;
    }
}
