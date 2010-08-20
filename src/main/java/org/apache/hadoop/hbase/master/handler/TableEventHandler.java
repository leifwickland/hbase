/**
 * Copyright 2010 The Apache Software Foundation
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.hadoop.hbase.master.handler;

import java.io.IOException;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.hbase.HRegionInfo;
import org.apache.hadoop.hbase.Server;
import org.apache.hadoop.hbase.TableNotDisabledException;
import org.apache.hadoop.hbase.TableNotFoundException;
import org.apache.hadoop.hbase.catalog.MetaReader;
import org.apache.hadoop.hbase.executor.EventHandler;
import org.apache.hadoop.hbase.master.MasterServices;
import org.apache.hadoop.hbase.util.Bytes;

/**
 * Base class for performing operations against tables.
 * <p>
 * Ensures all regions of the table are offline and then executes
 * {@link #handleTableOperation(List)} with a list of regions of the
 * table.
 */
public abstract class TableEventHandler extends EventHandler {
  private static final Log LOG = LogFactory.getLog(TableEventHandler.class);
  protected final MasterServices masterServices;
  protected final byte [] tableName;
  protected final String tableNameStr;

  public TableEventHandler(EventType eventType, byte [] tableName, Server server,
      MasterServices masterServices) {
    super(server, eventType);
    this.masterServices = masterServices;
    this.tableName = tableName;
    this.tableNameStr = Bytes.toString(this.tableName);
  }

  @Override
  public void process() {
    try {
      LOG.info("Handling table operation " + eventType + " on table " +
          Bytes.toString(tableName));
      handleTableOperation(tableChecks());
    } catch (IOException e) {
      LOG.error("Error trying to delete the table " + Bytes.toString(tableName),
          e);
    }
  }

  private List<HRegionInfo> tableChecks() throws IOException {
    // Check if table exists
    if (!MetaReader.tableExists(this.masterServices.getCatalogTracker(),
        this.tableNameStr)) {
      throw new TableNotFoundException(Bytes.toString(tableName));
    }
    // Verify table is offline
    if (!this.masterServices.getAssignmentManager().
        isTableDisabled(this.tableNameStr)) {
      throw new TableNotDisabledException(tableName);
    }
    // Get the regions of this table
    // TODO: Use in-memory state of master?
    return MetaReader.getTableRegions(this.masterServices.getCatalogTracker(),
      tableName);
  }

  protected abstract void handleTableOperation(List<HRegionInfo> regions)
  throws IOException;
}