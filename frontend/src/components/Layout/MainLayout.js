import { Layout } from "antd";
import Sidebar from "../Sidebar/Sidebar";

const { Content } = Layout;

export default function MainLayout({ children, breadcrumb = [] }) {

  return (
    <Layout style={{ minHeight: "100vh" }}>
      <Sidebar />
      <Layout style={{ marginLeft: 250 }}>
        <Content
          style={{
            margin: 24,
            padding: 24,
            background: "#fff",
            borderRadius: 8,
            boxShadow: "0 1px 3px rgba(0,0,0,0.12), 0 1px 2px rgba(0,0,0,0.24)",
          }}
        >
          {children}
        </Content>
      </Layout>
    </Layout>
  );
}
