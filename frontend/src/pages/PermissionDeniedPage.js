import React, { useEffect, useMemo } from "react";
import { Alert, Button, Card, Space, Typography } from "antd";
import { SafetyCertificateOutlined } from "@ant-design/icons";
import { useLocation, useNavigate } from "react-router-dom";

const { Paragraph, Title } = Typography;

export default function PermissionDeniedPage() {
  const navigate = useNavigate();
  const location = useLocation();

  const reason = useMemo(
    () =>
      location.state?.reason ||
      sessionStorage.getItem("permission_denied_reason") ||
      "当前账号没有执行该操作的权限，请联系管理员为你分配相应权限。",
    [location.state]
  );

  useEffect(() => {
    sessionStorage.removeItem("permission_denied_reason");
  }, []);

  return (
    <div
      style={{
        minHeight: "calc(100vh - 96px)",
        display: "flex",
        alignItems: "center",
        justifyContent: "center",
        padding: 24,
      }}
    >
      <Card style={{ width: "100%", maxWidth: 560, borderRadius: 16 }}>
        <Space direction="vertical" size={16} style={{ width: "100%" }}>
          <Space align="center">
            <SafetyCertificateOutlined style={{ fontSize: 28, color: "#d4380d" }} />
            <Title level={3} style={{ margin: 0 }}>
              无权限访问
            </Title>
          </Space>

          <Alert type="error" showIcon message="访问被拒绝" description={reason} />

          <Paragraph style={{ marginBottom: 0 }}>
            你可以停留在此页确认原因，或返回上一页继续操作。
          </Paragraph>

          <Space>
            <Button onClick={() => navigate(-1)}>返回上一页</Button>
            <Button type="primary" onClick={() => navigate("/home")}>
              返回首页
            </Button>
          </Space>
        </Space>
      </Card>
    </div>
  );
}
