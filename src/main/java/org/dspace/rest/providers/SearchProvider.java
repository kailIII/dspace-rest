/*
 * SearchProvider.java
 *
 * Version: $Revision$
 *
 * Date: $Date$
 *
 * Copyright (c) 2002-2009, The DSpace Foundation.  All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 * - Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 *
 * - Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 *
 * - Neither the name of the DSpace Foundation nor the names of its
 * contributors may be used to endorse or promote products derived from
 * this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * ``AS IS'' AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * HOLDERS OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS
 * OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR
 * TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE
 * USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH
 * DAMAGE.
 */
package org.dspace.rest.providers;

import java.util.ArrayList;
import java.util.List;
import org.sakaiproject.entitybus.EntityReference;
import org.sakaiproject.entitybus.entityprovider.CoreEntityProvider;
import org.sakaiproject.entitybus.entityprovider.EntityProviderManager;
import org.sakaiproject.entitybus.entityprovider.search.Search;
import org.dspace.core.Context;
import java.sql.SQLException;
import org.sakaiproject.entitybus.exception.EntityException;
import org.dspace.rest.entities.*;
import org.dspace.search.*;
import org.apache.log4j.Logger;
import org.dspace.sort.SortOption;
import org.dspace.core.Constants;
import java.io.IOException;
import java.util.Collections;
import org.dspace.rest.util.GenComparator;

/**
 * Enables users to search through items according to different criteria
 * @see SearchResultsInfoEntity
 * @author Bojan Suzic, bojan.suzic@gmail.com
 */
public class SearchProvider extends AbstractBaseProvider implements CoreEntityProvider {

    private static Logger log = Logger.getLogger(UserProvider.class);

    /**
     * Handles provider for search accross items
     * @param entityProviderManager
     * @throws java.sql.SQLException
     */
    public SearchProvider(EntityProviderManager entityProviderManager) throws SQLException {
        super(entityProviderManager);
        entityProviderManager.registerEntityProvider(this);
    }

    public String getEntityPrefix() {
        return "search";
    }

    public boolean entityExists(String id) {
        return true;
    }

    public Object getEntity(EntityReference reference) {
        log.info(userInfo() + "get_entity:" + reference.getId());
        throw new EntityException("Not Acceptable", "The data is not available", 406);
    }

    public List<?> getEntities(EntityReference ref, Search search) {
        log.info(userInfo() + "get_entities");

        Context context;
        try {
            context = new Context();
        } catch (SQLException ex) {
            throw new EntityException("Internal server error", "SQL error", 500);
        }

        // refresh parameters for this request
        refreshParams(context);
        List<Object> entities = new ArrayList<Object>();

        try {
            // extract query arguments from the request
            // deprecated - this is now handled at the end of function
            QueryArgs arg = new QueryArgs();
            arg.setQuery(query);

            if (_perpage > 0) {
                arg.setPageSize(_perpage);
            }
            arg.setStart(_start);

            if ((_order.equalsIgnoreCase("descending")) || (_order.equalsIgnoreCase("desc"))) {
                arg.setSortOrder(SortOption.DESCENDING);
            } else {
                arg.setSortOrder(SortOption.ASCENDING);
            }

            QueryResults qre;

            /**
             * search can be performed only on community or collection selected
             * or all, not on the both in same time; check this requirement
             */
            if (_community != null) {
                qre = DSQuery.doQuery(context, arg, _community);
            } else if (_collection != null) {
                qre = DSQuery.doQuery(context, arg, _collection);
            } else {
                qre = DSQuery.doQuery(context, arg);
            }
            entities.add(new SearchResultsInfoEntity(qre.getHitCount() - 1, qre.getHitTypes(), qre.getHitHandles(), qre.getHitIds()));

            /**
             * check returned objects, recognize them and put in result
             * list as expected
             */
            for (int x = 0; x < qre.getHitTypes().size(); x++) {
                switch ((Integer) (qre.getHitTypes().get(x))) {
                    case Constants.ITEM:
                         {
                            entities.add(idOnly ? new ItemEntityId(qre.getHitIds().get(x).toString(), context) : new ItemEntity(qre.getHitIds().get(x).toString(), context));
                        }
                        break;

                    case Constants.COMMUNITY:
                         {
                            entities.add(idOnly ? new CommunityEntityId(qre.getHitIds().get(x).toString(), context) : new CommunityEntity(qre.getHitIds().get(x).toString(), context));
                        }
                        break;

                    case Constants.COLLECTION:
                         {
                            entities.add(idOnly ? new CollectionEntityId(qre.getHitIds().get(x).toString(), context) : new CollectionEntity(qre.getHitIds().get(x).toString(), context));
                        }
                        break;

                    case Constants.BITSTREAM:
                         {
                            entities.add(idOnly ? new BitstreamEntityId(qre.getHitIds().get(x).toString(), context) : new BitstreamEntity(qre.getHitIds().get(x).toString(), context));
                        }
                        break;

                    case Constants.BUNDLE:
                         {
                            entities.add(idOnly ? new BundleEntityId(qre.getHitIds().get(x).toString(), context) : new BundleEntity(qre.getHitIds().get(x).toString(), context));
                        }
                        break;

                    case Constants.EPERSON:
                         {
                            entities.add(idOnly ? new UserEntityId(qre.getHitIds().get(x).toString(), context) : new UserEntity(qre.getHitIds().get(x).toString(), context));
                        }
                        break;

                }
            }

        } catch (SQLException sql) {
            throw new EntityException("Internal Server Error", "SQL Problem", 500);
        } catch (IOException io) {
            throw new EntityException("Internal Server Error", "Could not execute query", 500);
        }

        /**
         * if the full info are requested and there are sorting requirements
         * process entities through sorting filter first
         */
        if (!idOnly && sortOptions.size() > 0) {
            Collections.sort(entities, new GenComparator(sortOptions));
        }

        /**
         * process entities according to _limit, _perpage etc
         */
        removeTrailing(entities);

        return entities;
    }

    /**
     * Returns a Entity object with sample data
     */
    public Object getSampleEntity() {
        return null;
    }
}