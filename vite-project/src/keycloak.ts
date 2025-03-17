import Keycloak from "keycloak-js";

const keycloak = new Keycloak({
  url: "http://localhost:14082/",
  realm: "springboot-test",
  clientId: "react-app",
});

export default keycloak;