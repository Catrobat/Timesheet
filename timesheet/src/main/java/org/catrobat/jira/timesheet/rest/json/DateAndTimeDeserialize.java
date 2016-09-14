package org.catrobat.jira.timesheet.rest.json;

import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.map.DeserializationContext;
import org.codehaus.jackson.map.JsonDeserializer;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class DateAndTimeDeserialize extends JsonDeserializer<Date> {

    private SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    @Override
    public Date deserialize(JsonParser paramJsonParser, DeserializationContext paramDeserializationContext)
            throws IOException {
        String string = paramJsonParser.getText().trim();
        try {
            return dateFormat.parse(string);
        } catch (ParseException e) {

        }
        return paramDeserializationContext.parseDate(string);
    }
}