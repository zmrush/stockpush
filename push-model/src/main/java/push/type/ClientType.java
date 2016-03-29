package push.type;

public enum ClientType {

    WINDOWS(0), MAC(1), LINUX(2), IOS(3), ANDROID(4), WINPHONE(5), UNKNOUWN(-1);

    private byte mValue = 0;

    public byte value() {
        return mValue;
    }

    ClientType(int value) {
        mValue = (byte) value;
    }

    public static ClientType valueOfRaw(byte value) {
        for (ClientType clientType : ClientType.values()) {
            if (clientType.value() == value) {
                return clientType;
            }
        }
        return UNKNOUWN;
    }
}
