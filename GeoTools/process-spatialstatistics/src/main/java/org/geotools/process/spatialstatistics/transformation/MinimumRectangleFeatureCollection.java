/*
 *    GeoTools - The Open Source Java GIS Toolkit
 *    http://geotools.org
 *
 *    (C) 2014, Open Source Geospatial Foundation (OSGeo)
 *
 *    This library is free software; you can redistribute it and/or
 *    modify it under the terms of the GNU Lesser General Public
 *    License as published by the Free Software Foundation;
 *    version 2.1 of the License.
 *
 *    This library is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *    Lesser General Public License for more details.
 */
package org.geotools.process.spatialstatistics.transformation;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.logging.Logger;

import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.feature.collection.SubFeatureCollection;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.filter.function.StaticGeometry;
import org.geotools.process.spatialstatistics.core.FeatureTypes;
import org.geotools.util.logging.Logging;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Polygon;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.filter.Filter;

/**
 * Feature To Minimum Rectangle SimpleFeatureCollection Implementation
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
public class MinimumRectangleFeatureCollection extends GXTSimpleFeatureCollection {
    protected static final Logger LOGGER = Logging
            .getLogger(MinimumRectangleFeatureCollection.class);

    private boolean singlePart;

    private SimpleFeatureType schema;

    public MinimumRectangleFeatureCollection(SimpleFeatureCollection delegate, boolean singlePart) {
        super(delegate);

        this.singlePart = singlePart;

        String typeName = delegate.getSchema().getTypeName();
        this.schema = FeatureTypes.build(delegate.getSchema(), typeName, Polygon.class);
    }

    @Override
    public SimpleFeatureIterator features() {
        return new MinimumRectangleFeatureIterator(delegate.features(), getSchema(), singlePart);
    }

    @Override
    public SimpleFeatureType getSchema() {
        return schema;
    }

    @Override
    public SimpleFeatureCollection subCollection(Filter filter) {
        if (filter == Filter.INCLUDE) {
            return this;
        }
        return new SubFeatureCollection(this, filter);
    }

    static class MinimumRectangleFeatureIterator implements SimpleFeatureIterator {
        private SimpleFeatureIterator delegate;

        private boolean singlePart = false;

        private SimpleFeatureBuilder builder;

        public MinimumRectangleFeatureIterator(SimpleFeatureIterator delegate,
                SimpleFeatureType schema, boolean singlePart) {
            this.delegate = delegate;

            this.singlePart = singlePart;
            this.builder = new SimpleFeatureBuilder(schema);
        }

        public void close() {
            delegate.close();
        }

        public boolean hasNext() {
            return delegate.hasNext();
        }

        public SimpleFeature next() throws NoSuchElementException {
            SimpleFeature sourceFeature = delegate.next();

            for (Object attribute : sourceFeature.getAttributes()) {
                if (attribute instanceof Geometry) {
                    Geometry geometry = (Geometry) attribute;
                    if (!singlePart && geometry.getNumGeometries() > 1) {
                        List<Geometry> geomList = new ArrayList<Geometry>();
                        for (int index = 0; index < geometry.getNumGeometries(); index++) {
                            Geometry part = geometry.getGeometryN(index);
                            geomList.add(StaticGeometry.minimumRectangle(part));
                        }
                        attribute = geometry.getFactory().buildGeometry(geomList);
                    } else {
                        attribute = StaticGeometry.minimumRectangle(geometry);
                    }
                }
                builder.add(attribute);
            }

            SimpleFeature nextFeature = builder.buildFeature(sourceFeature.getID());
            builder.reset();

            return nextFeature;
        }
    }
}