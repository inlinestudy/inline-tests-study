package cl;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Contains all required BI data for MessageML instrumentation. It's composed by a list of {@link BiItem}, one per element
 * found inside the message along with the current messageML-utils version.
 */
public class A {
  /**
   * Used for elements where we want to increase attribute's value.
   * If the element does not exist in the context yet, it is put.
   * If the element exists in the context but no value found for the attribute, it is put.
   * If the element and its attribute exist in the context, then the value is increased.
   *
   * @param itemName name of the element to be checked
   * @param attributes map of attributes for the given element
   */
  public void updateItemCount(String itemName, Map<String, Object> attributes) {
    Optional<BiItem> optionalBiItem = getItemWithName(itemName);
    if (optionalBiItem.isPresent()) {
      attributes.forEach((key, value) -> {
        if (!StringUtils.isEmpty(String.valueOf(value))
            && optionalBiItem.get().getAttributes().get(key) != null) {
          optionalBiItem.get().increaseAttributeCount(key);
        } else { optionalBiItem.get().getAttributes().putIfAbsent(key, value); }
      });
    } else {
      addItem(new BiItem(itemName, attributes));
    }
  }
}

