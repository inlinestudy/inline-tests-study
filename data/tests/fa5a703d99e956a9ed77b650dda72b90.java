src/main/java/org/opensky/libadsb/msgs/AirspeedHeadingMsg.java;89;itest("", 89).given(msg_subtype, 0).checkFalse(group());
src/main/java/org/opensky/libadsb/msgs/AirspeedHeadingMsg.java;89;itest("", 89).given(msg_subtype, 4).checkTrue(group());
src/main/java/org/opensky/libadsb/msgs/AirspeedHeadingMsg.java;89;itest("", 89).given(airspeed, 1).checkEq(airspeed, 4);
src/main/java/org/opensky/libadsb/msgs/AirspeedHeadingMsg.java;89;itest("", 89).given(airspeed, 2).checkEq(airspeed, 8);
src/main/java/org/opensky/libadsb/msgs/AirspeedHeadingMsg.java;89;itest("", 89).given(airspeed, 100).checkEq(airspeed, 400);
