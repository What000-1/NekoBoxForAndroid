package moe.matsuri.nb4a.proxy.config;

import androidx.annotation.NonNull;

import com.esotericsoftware.kryo.io.ByteBufferInput;
import com.esotericsoftware.kryo.io.ByteBufferOutput;
import com.google.gson.JsonObject;

import org.jetbrains.annotations.NotNull;

import io.nekohasekai.sagernet.fmt.KryoConverters;
import io.nekohasekai.sagernet.fmt.internal.InternalBean;
import moe.matsuri.nb4a.utils.JavaUtil;

public class ConfigBean extends InternalBean {

    public Integer type; // 0=config 1=outbound
    public String config;
    public Integer coreType; // 0=sing-box, 1=mihomo, 2=xray
    public Integer localPort;

    @Override
    public void initializeDefaultValues() {
        super.initializeDefaultValues();
        if (type == null) type = 0;
        if (config == null) config = "";
        if (coreType == null) coreType = 0;
        if (localPort == null) localPort = 7890;
    }

    @Override
    public void serialize(ByteBufferOutput output) {
        output.writeInt(1);
        super.serialize(output);
        output.writeInt(type);
        output.writeString(config);
        output.writeInt(coreType);
        output.writeInt(localPort);
    }

    @Override
    public void deserialize(ByteBufferInput input) {
        int version = input.readInt();
        super.deserialize(input);
        type = input.readInt();
        config = input.readString();
        if (version >= 1) {
            coreType = input.readInt();
            localPort = input.readInt();
        } else {
            coreType = 0;
            localPort = 7890;
        }
    }

    @Override
    public String displayName() {
        if (JavaUtil.isNotBlank(name)) {
            return name;
        } else {
            return "Custom " + Math.abs(hashCode());
        }
    }

    public String displayType() {
        if (type != null && type == 1 && JavaUtil.isNotBlank(config)) {
            try {
                JsonObject json = JavaUtil.gson.fromJson(config, JsonObject.class);
                if (json != null && json.has("type")) {
                    return json.get("type").getAsString() + " (sing-box)";
                }
            } catch (Exception ignored) {
            }
        }
        if (type != null && type == 0) {
            if (coreType != null && coreType == 1) return "mihomo config";
            if (coreType != null && coreType == 2) return "xray config";
            return "sing-box config";
        }
        return "sing-box outbound";
    }

    @NotNull
    @Override
    public ConfigBean clone() {
        return KryoConverters.deserialize(new ConfigBean(), KryoConverters.serialize(this));
    }

    public static final Creator<ConfigBean> CREATOR = new CREATOR<ConfigBean>() {
        @NonNull
        @Override
        public ConfigBean newInstance() {
            return new ConfigBean();
        }

        @Override
        public ConfigBean[] newArray(int size) {
            return new ConfigBean[size];
        }
    };
}