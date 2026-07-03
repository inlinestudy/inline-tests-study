package test;

import static com.restfb.util.StringUtils.isBlank;

public class U {

    private String birthday;

    public String getBirthday() {
        return this.birthday;
    }

    public Date getBirthdayAsDate() {
        if (isBlank(getBirthday()) || getBirthday().split("/").length < 2) {
            return null;
        }
        return toDateFromShortFormat(birthday);
    }

}
