/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.inlong.sort.cdc.mysql.source.split;

import io.debezium.relational.TableId;
import io.debezium.relational.history.TableChanges.TableChange;
import org.apache.inlong.sort.cdc.mysql.source.offset.BinlogOffset;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * The split to describe the binlog of MySql table(s).
 */
public class MySqlBinlogSplit extends MySqlSplit {

    private final BinlogOffset startingOffset;
    private final BinlogOffset endingOffset;
    private final List<FinishedSnapshotSplitInfo> finishedSnapshotSplitInfos;
    private final Map<TableId, TableChange> tableSchemas;
    private final int totalFinishedSplitSize;
    private final boolean isSuspended;
    @Nullable
    transient byte[] serializedFormCache;
    private final Map<TableId, String> tableDdls;

    public MySqlBinlogSplit(
            String splitId,
            BinlogOffset startingOffset,
            BinlogOffset endingOffset,
            List<FinishedSnapshotSplitInfo> finishedSnapshotSplitInfos,
            Map<TableId, TableChange> tableSchemas,
            int totalFinishedSplitSize,
            boolean isSuspended,
            Map<TableId, String> tableDdls) {
        super(splitId);
        this.startingOffset = startingOffset;
        this.endingOffset = endingOffset;
        this.finishedSnapshotSplitInfos = finishedSnapshotSplitInfos;
        this.tableSchemas = tableSchemas;
        this.totalFinishedSplitSize = totalFinishedSplitSize;
        this.isSuspended = isSuspended;
        this.tableDdls = tableDdls;
    }

    public MySqlBinlogSplit(
            String splitId,
            BinlogOffset startingOffset,
            BinlogOffset endingOffset,
            List<FinishedSnapshotSplitInfo> finishedSnapshotSplitInfos,
            Map<TableId, TableChange> tableSchemas,
            int totalFinishedSplitSize) {
        super(splitId);
        this.startingOffset = startingOffset;
        this.endingOffset = endingOffset;
        this.finishedSnapshotSplitInfos = finishedSnapshotSplitInfos;
        this.tableSchemas = tableSchemas;
        this.totalFinishedSplitSize = totalFinishedSplitSize;
        this.isSuspended = false;
        this.tableDdls = new HashMap<>();
    }

    // -------------------------------------------------------------------
    // factory utils to build new MySqlBinlogSplit instance
    // -------------------------------------------------------------------
    public static MySqlBinlogSplit appendFinishedSplitInfos(
            MySqlBinlogSplit binlogSplit, List<FinishedSnapshotSplitInfo> splitInfos) {
        splitInfos.addAll(binlogSplit.getFinishedSnapshotSplitInfos());
        return new MySqlBinlogSplit(
                binlogSplit.splitId,
                binlogSplit.getStartingOffset(),
                binlogSplit.getEndingOffset(),
                splitInfos,
                binlogSplit.getTableSchemas(),
                binlogSplit.getTotalFinishedSplitSize(),
                binlogSplit.isSuspended(),
                binlogSplit.getTableDdls());
    }

    public static MySqlBinlogSplit fillTableSchemas(
            MySqlBinlogSplit binlogSplit, Map<TableId, TableChange> tableSchemas) {
        tableSchemas.putAll(binlogSplit.getTableSchemas());
        return new MySqlBinlogSplit(
                binlogSplit.splitId,
                binlogSplit.getStartingOffset(),
                binlogSplit.getEndingOffset(),
                binlogSplit.getFinishedSnapshotSplitInfos(),
                tableSchemas,
                binlogSplit.getTotalFinishedSplitSize(),
                binlogSplit.isSuspended(),
                binlogSplit.getTableDdls());
    }

    public static MySqlBinlogSplit toNormalBinlogSplit(
            MySqlBinlogSplit suspendedBinlogSplit, int totalFinishedSplitSize) {
        return new MySqlBinlogSplit(
                suspendedBinlogSplit.splitId,
                suspendedBinlogSplit.getStartingOffset(),
                suspendedBinlogSplit.getEndingOffset(),
                suspendedBinlogSplit.getFinishedSnapshotSplitInfos(),
                suspendedBinlogSplit.getTableSchemas(),
                totalFinishedSplitSize,
                false,
                suspendedBinlogSplit.getTableDdls());
    }

    public static MySqlBinlogSplit toSuspendedBinlogSplit(MySqlBinlogSplit normalBinlogSplit) {
        return new MySqlBinlogSplit(
                normalBinlogSplit.splitId,
                normalBinlogSplit.getStartingOffset(),
                normalBinlogSplit.getEndingOffset(),
                new ArrayList<>(),
                new HashMap<>(),
                normalBinlogSplit.getTotalFinishedSplitSize(),
                true,
                normalBinlogSplit.getTableDdls());
    }

    public BinlogOffset getStartingOffset() {
        return startingOffset;
    }

    public BinlogOffset getEndingOffset() {
        return endingOffset;
    }

    public List<FinishedSnapshotSplitInfo> getFinishedSnapshotSplitInfos() {
        return finishedSnapshotSplitInfos;
    }

    @Override
    public Map<TableId, TableChange> getTableSchemas() {
        return tableSchemas;
    }

    public int getTotalFinishedSplitSize() {
        return totalFinishedSplitSize;
    }

    public boolean isSuspended() {
        return isSuspended;
    }

    public boolean isCompletedSplit() {
        return totalFinishedSplitSize == finishedSnapshotSplitInfos.size();
    }

    public Map<TableId, String> getTableDdls() {
        return tableDdls;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof MySqlBinlogSplit)) {
            return false;
        }
        if (!super.equals(o)) {
            return false;
        }
        MySqlBinlogSplit that = (MySqlBinlogSplit) o;
        return totalFinishedSplitSize == that.totalFinishedSplitSize
                && isSuspended == that.isSuspended
                && Objects.equals(startingOffset, that.startingOffset)
                && Objects.equals(endingOffset, that.endingOffset)
                && Objects.equals(finishedSnapshotSplitInfos, that.finishedSnapshotSplitInfos)
                && Objects.equals(tableSchemas, that.tableSchemas);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                super.hashCode(),
                startingOffset,
                endingOffset,
                finishedSnapshotSplitInfos,
                tableSchemas,
                totalFinishedSplitSize,
                isSuspended);
    }

    @Override
    public String toString() {
        return "MySqlBinlogSplit{"
                + "splitId='"
                + splitId
                + '\''
                + ", offset="
                + startingOffset
                + ", endOffset="
                + endingOffset
                + ", isSuspended="
                + isSuspended
                + '}';
    }
}
