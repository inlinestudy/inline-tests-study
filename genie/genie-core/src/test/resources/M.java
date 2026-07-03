package test;

import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;
import java.util.Properties;
import java.util.Stack;
import com.tngtech.propertyloader.exception.PropertyLoaderException;
import com.tngtech.propertyloader.impl.DefaultPropertyFilterContainer;
import com.tngtech.propertyloader.impl.DefaultPropertyLocationContainer;
import com.tngtech.propertyloader.impl.DefaultPropertySuffixContainer;
import com.tngtech.propertyloader.impl.PropertyFileReader;
import com.tngtech.propertyloader.impl.PropertyLoaderFactory;
import com.tngtech.propertyloader.impl.helpers.HostsHelper;
import com.tngtech.propertyloader.impl.helpers.PropertyFileNameHelper;
import com.tngtech.propertyloader.impl.interfaces.PropertyFilterContainer;
import com.tngtech.propertyloader.impl.interfaces.PropertyLoaderFilter;
import com.tngtech.propertyloader.impl.interfaces.PropertyLoaderOpener;
import com.tngtech.propertyloader.impl.interfaces.PropertyLocationsContainer;
import com.tngtech.propertyloader.impl.interfaces.PropertySuffixContainer;
import static org.inlinetest.ITest.itest;
import static org.inlinetest.ITest.group;

public class M {
    private static final String INCLUDE_KEY = "$include";

    private String[] collectIncludesAndRemoveKey(Properties properties) {
        String[] includes = new String[] {};
        if (properties.containsKey(INCLUDE_KEY)) {
            includes = properties.getProperty(INCLUDE_KEY).split(",");
            properties.remove(INCLUDE_KEY);
        }
        return includes;
    }
}