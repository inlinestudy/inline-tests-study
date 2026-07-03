src/main/java/org/opensky/libadsb/msgs/LongACAS.java;84;itest("", 84).given(payload,new byte[] {0, 0, 0, 0, 0, 0, 0, 0}).checkEq(active_resolution_advisories,0);
src/main/java/org/opensky/libadsb/msgs/LongACAS.java;84;itest("", 84).given(payload,new byte[] {85, 53, -114, 53, 53, 0, 4, -114}).checkEq(active_resolution_advisories,3392);
src/main/java/org/opensky/libadsb/msgs/LongACAS.java;84;itest("", 84).given(payload,new byte[] {-1, 53, -114, 85, -128, 4, -127, 1, -1}).checkEq(active_resolution_advisories,8193);
src/main/java/org/opensky/libadsb/msgs/LongACAS.java;84;itest("", 84).given(payload,new byte[] {-1, 122, 0, -1, -2, -70, -57}).checkEq(active_resolution_advisories,16258);
