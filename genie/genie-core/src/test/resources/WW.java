package test;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.ArrayUtils;
import org.craftercms.core.exception.XmlMergeException;
import org.craftercms.core.service.CachingOptions;
import org.craftercms.core.service.Context;
import org.craftercms.core.util.ContentStoreUtils;
import org.craftercms.core.xml.mergers.DescriptorMergeStrategy;
import org.craftercms.core.xml.mergers.MergeableDescriptor;
import org.dom4j.Document;

public class WW {
    public List<MergeableDescriptor> getDescriptors(Context context, CachingOptions cachingOptions,
                                                    String mainDescriptorUrl, Document mainDescriptorDom,
                                                    boolean mainDescriptorOptional) throws XmlMergeException {
        List<MergeableDescriptor> descriptors = new ArrayList<>();

        int k;

        if (ArrayUtils.isNotEmpty(baseFolders)) {
            // If base folders are specified, start looking for descriptors after the base folders.
            k = getIndexAfterBaseFolder(mainDescriptorUrl);
        } else {
            // If the url is absolute (starts with '/'), the descriptors included will start from root (i.e. if url is
            // /folder/file.xml, first ones will start at '/'). If it's relative (doesn't start with '/), the descriptors
            // included start from the first folder in the url (i.e., if url is folder/file.xml, first ones will start at
            // folder/).
            k = mainDescriptorUrl.indexOf('/');
        }

        while (k >= 0) {
            String folder = mainDescriptorUrl.substring(0, k);

            addInheritedDescriptorsInFolder(context, cachingOptions, descriptors, folder, mainDescriptorUrl,
                                            mainDescriptorDom);

            k = mainDescriptorUrl.indexOf('/', ++k);
        }

        descriptors.add(new MergeableDescriptor(mainDescriptorUrl, mainDescriptorOptional));

        return descriptors;
    }

}
