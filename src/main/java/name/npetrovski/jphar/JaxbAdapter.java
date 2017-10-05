package name.npetrovski.jphar;

import lombok.Data;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.adapters.XmlAdapter;
import java.util.List;

public class JaxbAdapter extends XmlAdapter<JaxbAdapter.PharMapper, Phar> {

    @Data
    @XmlAccessorType(XmlAccessType.FIELD)
    static class PharMapper {

        @XmlElement
        private String fullpath;

        @XmlElement
        private Stub stub;

        @XmlElement
        private Manifest manifest;

        @XmlElementWrapper(name = "data")
        @XmlElement(name = "entry")
        private List<DataEntry> entries;

        @XmlElement
        private Signature signature;

    }

    @Override
    public Phar unmarshal(PharMapper map) throws Exception {
        Phar result = new Phar(map.getFullpath());
        result.setStub(map.getStub().getCode());
        result.setManifest(map.getManifest());
        result.setEntries(map.getEntries());
        result.setSignature(map.getSignature());

        return result;
    }

    @Override
    public PharMapper marshal(Phar phar) throws Exception {
        PharMapper result = new PharMapper();
        result.setFullpath(phar.getCanonicalPath());
        result.setStub(phar.getStub());
        result.setManifest(phar.getManifest());
        result.setEntries(phar.getEntries());
        result.setSignature(phar.getSignature());

        return result;
    }

}
