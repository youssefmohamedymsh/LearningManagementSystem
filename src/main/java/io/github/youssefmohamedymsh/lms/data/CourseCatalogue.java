package io.github.youssefmohamedymsh.lms.data;

import io.github.youssefmohamedymsh.lms.model.Course;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/** Loads the course catalogue from XML. */
public final class CourseCatalogue {

    private CourseCatalogue() {
    }

    public static List<Course> fromXml(InputStream xml) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true); // no XXE
            Document doc = factory.newDocumentBuilder().parse(xml);

            // Read each field from its own <row>, so a missing tag in one row can't shift every row after it.
            NodeList rows = doc.getElementsByTagName("row");
            List<Course> courses = new ArrayList<>();
            for (int i = 0; i < rows.getLength(); i++) {
                Element row = (Element) rows.item(i);
                courses.add(new Course(
                        Integer.parseInt(text(row, "id")),
                        text(row, "CourseName"),
                        text(row, "Instructor"),
                        text(row, "CourseDuration"),
                        text(row, "CourseTime"),
                        text(row, "Location")));
            }
            return courses;
        } catch (Exception e) {
            throw new IllegalStateException("Couldn't read the course catalogue: " + e.getMessage(), e);
        }
    }

    private static String text(Element row, String tag) {
        NodeList nodes = row.getElementsByTagName(tag);
        if (nodes.getLength() == 0) {
            throw new IllegalStateException("A course row is missing <" + tag + ">");
        }
        return nodes.item(0).getTextContent().trim();
    }
}
