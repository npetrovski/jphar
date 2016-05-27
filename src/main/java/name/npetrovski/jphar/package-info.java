@XmlJavaTypeAdapters({
    @XmlJavaTypeAdapter(
            value = name.npetrovski.jphar.jaxb.PharAdapter.class,
            type = name.npetrovski.jphar.Phar.class
    )})
package name.npetrovski.jphar.jaxb;

import javax.xml.bind.annotation.adapters.*;
