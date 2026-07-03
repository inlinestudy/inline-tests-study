package test;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Map;
import java.util.regex.Matcher;

public class EEE {

    private Map<String, String> ResolveTemplateParameters(Route route, StringBuffer urlBuffer, Object requestDto) {
        ArrayList<Field> fields = Func.toList(Utils.getSerializableFields(requestDto.getClass()));
        try {
            Matcher m = templateParameterPattern.matcher(route.Path());
            while (m.find()) {
                String parameterName = m.group(1);
            }
        } catch (IllegalAccessException | UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }
}
