import { useKeycloak } from "@react-keycloak/web";
import { useEffect, useState } from "react";
const Admin = () => {
    const { keycloak } = useKeycloak();
    const [adminMessage, setAdminMessage] = useState("");

    useEffect(() => {
        const getAdminData = async () => {
            try {
                if (keycloak && keycloak.authenticated) {
                    await keycloak?.updateToken();
                    const req = await fetch("http://localhost:1291/admin", {
                        headers: {
                            ["Authorization"]: `Bearer ${keycloak.token}`,
                        },
                    });
                    setAdminMessage(await req.text());
                }
            } catch (e) {
                console.log("ERROR", e);
            }
        };
        getAdminData();
    }, [keycloak]);

    return (
        <div>
            <h2>Admin Endpoint</h2>
            <p>{adminMessage}</p>
        </div>
    );
};

export default Admin;