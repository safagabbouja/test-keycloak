import { useEffect } from "react";
import "./App.css";
import Cars from "./Cars";
import Admin from "./Admin";

import keycloak from "./keycloak";
import { ReactKeycloakProvider, useKeycloak } from "@react-keycloak/web";

function App() {
  return (
    <ReactKeycloakProvider authClient={keycloak}>
      <SecuredContent />
    </ReactKeycloakProvider>
  );
}
const SecuredContent = () => {
  const { keycloak } = useKeycloak();
  const isLoggedIn = keycloak.authenticated;
  useEffect(() => {
    if (isLoggedIn === false) keycloak?.login();
  }, [isLoggedIn, keycloak]);
  if (!isLoggedIn) return <div>Not logged in</div>;
  return (
    <div>
      <h2>Springboot app using Keycloak authentication provider</h2>
      <Cars />
      <Admin />

    </div>
  );
};
export default App;