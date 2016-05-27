package name.npetrovski.jphar.jaxb;

import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.adapters.XmlAdapter;
import lombok.Data;
import name.npetrovski.jphar.DataEntry;
import name.npetrovski.jphar.Manifest;
import name.npetrovski.jphar.Phar;
import name.npetrovski.jphar.Signature;
import name.npetrovski.jphar.Stub;

public class PharAdapter extends XmlAdapter<PharAdapter.PharMapper, Phar> {

    @Data
    @XmlAccessorType(XmlAccessType.FIELD)
    @XmlType(propOrder = {"pathname", "stub", "manifest", "entries", "signature"})
    static class PharMapper {

        @XmlElement
        private String pathname;

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
        Phar result = new Phar(map.getPathname());
        result.setStub(map.getStub().getCode());
        result.setManifest(map.getManifest());
        result.setEntries(map.getEntries());
        result.setSignature(map.getSignature());

        return result;
    }

    @Override
    public PharMapper marshal(Phar phar) throws Exception {
        PharMapper result = new PharMapper();
        result.setPathname(phar.getPath());
        result.setStub(phar.getStub());
        result.setManifest(phar.getManifest());
        result.setEntries(phar.getEntries());
        result.setSignature(phar.getSignature());

        return result;
    }

}
