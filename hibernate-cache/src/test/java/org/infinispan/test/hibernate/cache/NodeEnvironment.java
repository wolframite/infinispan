/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.infinispan.test.hibernate.cache;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.infinispan.hibernate.cache.InfinispanRegionFactory;
import org.infinispan.hibernate.cache.collection.CollectionRegionImpl;
import org.infinispan.hibernate.cache.entity.EntityRegionImpl;
import org.hibernate.cache.spi.CacheDataDescription;

import org.infinispan.test.hibernate.cache.util.CacheTestUtil;

/**
 * Defines the environment for a node.
 *
 * @author Steve Ebersole
 */
public class NodeEnvironment {
   private final StandardServiceRegistryBuilder ssrb;
   private final Properties properties;

   private StandardServiceRegistry serviceRegistry;
   private InfinispanRegionFactory regionFactory;

   private Map<String, EntityRegionImpl> entityRegionMap;
   private Map<String, CollectionRegionImpl> collectionRegionMap;

   public NodeEnvironment(StandardServiceRegistryBuilder ssrb) {
      this.ssrb = ssrb;
      properties = CacheTestUtil.toProperties( ssrb.getSettings() );
   }

   public StandardServiceRegistry getServiceRegistry() {
      return serviceRegistry;
   }

   public EntityRegionImpl getEntityRegion(String name, CacheDataDescription cacheDataDescription) {
      if (entityRegionMap == null) {
         entityRegionMap = new HashMap<String, EntityRegionImpl>();
         return buildAndStoreEntityRegion(name, cacheDataDescription);
      }
      EntityRegionImpl region = entityRegionMap.get(name);
      if (region == null) {
         region = buildAndStoreEntityRegion(name, cacheDataDescription);
      }
      return region;
   }

   private EntityRegionImpl buildAndStoreEntityRegion(String name, CacheDataDescription cacheDataDescription) {
      EntityRegionImpl region = (EntityRegionImpl) regionFactory.buildEntityRegion(
            name,
            properties,
            cacheDataDescription
      );
      entityRegionMap.put(name, region);
      return region;
   }

   public CollectionRegionImpl getCollectionRegion(String name, CacheDataDescription cacheDataDescription) {
      if (collectionRegionMap == null) {
         collectionRegionMap = new HashMap<String, CollectionRegionImpl>();
         return buildAndStoreCollectionRegion(name, cacheDataDescription);
      }
      CollectionRegionImpl region = collectionRegionMap.get(name);
      if (region == null) {
         region = buildAndStoreCollectionRegion(name, cacheDataDescription);
         collectionRegionMap.put(name, region);
      }
      return region;
   }

   private CollectionRegionImpl buildAndStoreCollectionRegion(String name, CacheDataDescription cacheDataDescription) {
      CollectionRegionImpl region;
      region = (CollectionRegionImpl) regionFactory.buildCollectionRegion(
            name,
            properties,
            cacheDataDescription
      );
      return region;
   }

   public void prepare() throws Exception {
      serviceRegistry = ssrb.build();
      regionFactory = CacheTestUtil.startRegionFactory( serviceRegistry );
   }

   public void release() throws Exception {
      try {
         if (entityRegionMap != null) {
            for (EntityRegionImpl region : entityRegionMap.values()) {
               try {
                  region.getCache().stop();
               } catch (Exception e) {
                  // Ignore...
               }
            }
            entityRegionMap.clear();
         }
         if (collectionRegionMap != null) {
            for (CollectionRegionImpl reg : collectionRegionMap.values()) {
               try {
                  reg.getCache().stop();
               } catch (Exception e) {
                  // Ignore...
               }
            }
            collectionRegionMap.clear();
         }
      }
      finally {
         try {
            if (regionFactory != null) {
               // Currently the RegionFactory is shutdown by its registration
               // with the CacheTestSetup from CacheTestUtil when built
               regionFactory.stop();
            }
         }
         finally {
            if (serviceRegistry != null) {
               StandardServiceRegistryBuilder.destroy( serviceRegistry );
            }
         }
      }
   }
}
