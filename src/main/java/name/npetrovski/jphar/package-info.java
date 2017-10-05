@XmlJavaTypeAdapters({
    @XmlJavaTypeAdapter(
            value = JaxbAdapter.class,
            type = Phar.class
    )})
package name.npetrovski.jphar;

import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapters;
