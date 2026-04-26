import React, { useEffect, useMemo, useState } from "react";
import { Input, Select } from "antd";
import CRUDTable from "../../components/Common/CRUDTable";
import { customerAPI } from "../../api/modules";

const { Option } = Select;

export default function CustomerContacts() {
  const [contacts, setContacts] = useState([]);
  const [customers, setCustomers] = useState([]);
  const [loading, setLoading] = useState(false);

  const customerMap = useMemo(
    () => Object.fromEntries(customers.map((item) => [item.uuid, item.customerName])),
    [customers]
  );

  const decoratedContacts = useMemo(
    () =>
      contacts.map((item) => ({
        ...item,
        customerLabel: customerMap[item.clientId] || item.clientId,
      })),
    [contacts, customerMap]
  );

  const fetchContacts = async () => {
    setLoading(true);
    try {
      const response = await customerAPI.getCustomerContacts();
      setContacts(response.data || []);
    } finally {
      setLoading(false);
    }
  };

  const fetchCustomers = async () => {
    const response = await customerAPI.getCustomers();
    setCustomers(response.data || []);
  };

  useEffect(() => {
    fetchContacts();
    fetchCustomers();
  }, []);

  const handleCreate = async (values) => {
    await customerAPI.createCustomerContact(values);
    await fetchContacts();
  };

  const handleUpdate = async (uuid, values) => {
    await customerAPI.updateCustomerContact(uuid, values);
    await fetchContacts();
  };

  const handleDelete = async (uuid) => {
    await customerAPI.deleteCustomerContact(uuid);
    await fetchContacts();
  };

  return (
    <CRUDTable
      title="客户联系人"
      columns={[
        { title: "客户", dataIndex: "customerLabel", key: "customerLabel", width: 220 },
        { title: "姓名", dataIndex: "contactName", key: "contactName", width: 160 },
        { title: "职位", dataIndex: "contactPosition", key: "contactPosition", width: 160 },
        { title: "电话", dataIndex: "contactPhone", key: "contactPhone", width: 160 },
        { title: "微信", dataIndex: "wechat", key: "wechat", width: 160 },
        { title: "邮箱", dataIndex: "email", key: "email", width: 220 },
        { title: "QQ", dataIndex: "qq", key: "qq", width: 140 },
        { title: "备注", dataIndex: "remark", key: "remark", width: 220, ellipsis: true },
        {
          title: "更新时间",
          dataIndex: "updateTime",
          key: "updateTime",
          width: 180,
          render: (value) => (value ? new Date(value).toLocaleString() : "-"),
        },
      ]}
      dataSource={decoratedContacts}
      loading={loading}
      onCreate={handleCreate}
      onUpdate={handleUpdate}
      onDelete={handleDelete}
      formFields={[
        {
          name: "clientId",
          label: "客户",
          rules: [{ required: true, message: "请选择客户" }],
          component: (
            <Select placeholder="请选择客户" showSearch optionFilterProp="children">
              {customers.map((item) => (
                <Option key={item.uuid} value={item.uuid}>
                  {item.customerName}
                </Option>
              ))}
            </Select>
          ),
        },
        {
          name: "contactName",
          label: "姓名",
          rules: [{ required: true, message: "请输入联系人姓名" }],
          component: <Input placeholder="请输入联系人姓名" />,
        },
        {
          name: "contactPosition",
          label: "职位",
          component: <Input placeholder="请输入职位" />,
        },
        {
          name: "contactPhone",
          label: "电话",
          component: <Input placeholder="请输入联系电话" />,
        },
        {
          name: "wechat",
          label: "微信",
          component: <Input placeholder="请输入微信" />,
        },
        {
          name: "email",
          label: "邮箱",
          rules: [{ type: "email", message: "邮箱格式不正确" }],
          component: <Input placeholder="请输入邮箱" />,
        },
        {
          name: "qq",
          label: "QQ",
          component: <Input placeholder="请输入 QQ" />,
        },
        {
          name: "remark",
          label: "备注",
          component: <Input.TextArea placeholder="请输入备注" rows={3} />,
        },
      ]}
      searchFields={[
        { name: "customerLabel", label: "客户" },
        { name: "contactName", label: "姓名" },
        { name: "contactPhone", label: "电话" },
        { name: "wechat", label: "微信" },
        { name: "email", label: "邮箱" },
      ]}
      rowKey="uuid"
      transformValues={(values) => ({
        ...values,
        contactPosition: values.contactPosition || null,
        contactPhone: values.contactPhone || null,
        wechat: values.wechat || null,
        email: values.email || null,
        qq: values.qq || null,
        remark: values.remark || null,
      })}
    />
  );
}
