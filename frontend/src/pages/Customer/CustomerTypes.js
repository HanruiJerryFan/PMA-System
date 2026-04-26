import React, { useEffect, useState } from "react";
import { Tag } from "antd";
import CRUDTable from "../../components/Common/CRUDTable";
import { customerAPI } from "../../api/modules";

export default function CustomerTypes() {
  const [customerTypes, setCustomerTypes] = useState([]);
  const [loading, setLoading] = useState(false);

  const fetchCustomerTypes = async () => {
    setLoading(true);
    try {
      const response = await customerAPI.getCustomerTypes();
      setCustomerTypes(response.data || []);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchCustomerTypes();
  }, []);

  return (
    <CRUDTable
      title="客户类型"
      columns={[
        { title: "ID", dataIndex: "id", key: "id", width: 100 },
        {
          title: "类型编码",
          dataIndex: "typeCode",
          key: "typeCode",
          width: 120,
          render: (value) => <Tag color="gold">{value}</Tag>,
        },
        {
          title: "类型名称",
          dataIndex: "typeName",
          key: "typeName",
          width: 240,
          render: (value) => <Tag color="blue">{value}</Tag>,
        },
      ]}
      dataSource={customerTypes}
      loading={loading}
      rowKey="id"
      searchFields={[{ name: "typeName", label: "类型名称" }]}
    />
  );
}
