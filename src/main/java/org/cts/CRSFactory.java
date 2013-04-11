/*
 * Coordinate Transformations Suite (abridged CTS)  is a library developped to 
 * perform Coordinate Transformations using well known geodetic algorithms 
 * and parameter sets. 
 * Its main focus are simplicity, flexibility, interoperability, in this order.
 *
 * This library has been originaled developed by Michael Michaud under the JGeod
 * name. It has been renamed CTS in 2009 and shared to the community from 
 * the Atelier SIG code repository.
 * 
 * Since them, CTS is supported by the Atelier SIG team in collaboration with Michael 
 * Michaud.
 * The new CTS has been funded  by the French Agence Nationale de la Recherche 
 * (ANR) under contract ANR-08-VILL-0005-01 and the regional council 
 * "Région Pays de La Loire" under the projet SOGVILLE (Système d'Orbservation 
 * Géographique de la Ville).
 *
 * CTS is free software: you can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 *
 * CTS is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with
 * CTS. If not, see <http://www.gnu.org/licenses/>.
 *
 * For more information, please consult: <https://github.com/irstv/cts/>
 */
package org.cts;

import java.io.*;
import java.util.LinkedHashMap;
import java.util.Map;
import org.cts.crs.CRSException;
import org.cts.crs.CRSHelper;
import org.cts.crs.CoordinateReferenceSystem;
import org.cts.parser.prj.PrjParser;
import org.cts.registry.Registry;
import org.cts.registry.RegistryManager;

/**
 *
 * @author Erwan Bocher
 */
public class CRSFactory {

    private RegistryManager registryManager = new RegistryManager();
    protected final CRSCache<String, CoordinateReferenceSystem> CRSPOOL = new CRSCache<String, CoordinateReferenceSystem>(10);

    /**
     * Creates a new factory.
     */
    public CRSFactory() {
    }

    /**
     * Return a @CoordinateReferenceSystem according an authority and srid ie :
     * EPSG:4326
     *
     * @param authorityAndSrid
     * @return
     * @throws CRSException
     */
    public CoordinateReferenceSystem getCRS(String authorityAndSrid) throws CRSException {
        CoordinateReferenceSystem crs = CRSPOOL.get(authorityAndSrid);
        if (crs == null) {
            if (isRegistrySupported(authorityAndSrid)) {
                String[] registryNameWithCode = authorityAndSrid.split(":");
                Registry registry = getRegistryManager().getRegistry(registryNameWithCode[0]);
                Map<String, String> crsParameters = registry.getParameters(registryNameWithCode[1]);
                if (crsParameters != null) {
                    crs = CRSHelper.createCoordinateReferenceSystem(new Identifier(registryNameWithCode[0], registryNameWithCode[1], authorityAndSrid), crsParameters);
                }
                if (crs != null) {
                    CRSPOOL.put(authorityAndSrid, crs);
                }
            }
        }
        return crs;
    }

    /**
     * Return the registry manager
     *
     * @return
     */
    public RegistryManager getRegistryManager() {
        return registryManager;
    }

    /**
     * Check if the registry name of the crsCode is supported.
     *
     * @param crsCode
     * @return
     */
    public boolean isRegistrySupported(String crsCode) {
        int p = crsCode.indexOf(':');
        if (p >= 0) {
            String auth = crsCode.substring(0, p);
            if (getRegistryManager().contains(auth.toLowerCase())) {
                return true;
            }
        }
        throw new RuntimeException("This registry is not supported");
    }

    /**
     * Creates a {@link CoordinateReferenceSystem} defined by an OGC WKT String
     * (PRJ).
     *
     * @param prjString the PRJ String
     * @return a {@link CoordinateReferenceSystem}
     * @throws UnsupportedParameterException if a PROJ.4 parameter is not
     * supported
     * @throws InvalidValueException if a parameter value is invalid
     */
    public CoordinateReferenceSystem createFromPrj(String prjString) {
        PrjParser p = new PrjParser();
        return p.parse(prjString);
    }

    /**
     * Creates a {@link CoordinateReferenceSystem} defined by an OGC WKT String
     * (PRJ).
     *
     * @param stream
     * @return a {@link CoordinateReferenceSystem}
     * @throws UnsupportedParameterException if a PROJ.4 parameter is not
     * supported
     * @throws IOException
     * @throws InvalidValueException if a parameter value is invalid
     */
    public CoordinateReferenceSystem createFromPrj(InputStream stream) throws IOException {
        BufferedReader r = new BufferedReader(new InputStreamReader(stream));
        StringBuilder b = new StringBuilder();
        while (r.ready()) {
            b.append(r.readLine());
        }

        return createFromPrj(b.toString());
    }

    /**
     * Creates a {@link CoordinateReferenceSystem} defined by an OGC WKT String
     * (PRJ).
     *
     * @param file
     * @return a {@link CoordinateReferenceSystem}
     * @throws UnsupportedParameterException if a PROJ.4 parameter is not
     * supported
     * @throws IOException if there is a problem reading the file
     * @throws InvalidValueException if a parameter value is invalid
     */
    public CoordinateReferenceSystem createFromPrj(File file) throws IOException {
        InputStream i = null;
        CoordinateReferenceSystem crs;
        try {
            i = new FileInputStream(file);
            crs = createFromPrj(i);
        } finally {
            if (i != null) {
                i.close();
            }
        }

        return crs;
    }

    /**
     * A simple cache to manage {@link CoordinateReferenceSystem}
     *
     * @param <K>
     * @param <V>
     * @param maxSize
     * @return
     */
    public class CRSCache<K, V> extends LinkedHashMap<K, V> {

        private final int limit;

        public CRSCache(int limit) {
            super(16, 0.75f, true);
            this.limit = limit;
        }

        @Override
        protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
            return size() > limit;
        }
    }
}