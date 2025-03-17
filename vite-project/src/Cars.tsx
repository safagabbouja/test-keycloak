import { useKeycloak } from "@react-keycloak/web";
import { useEffect, useState } from "react";

type Car = {
  name: string;
  color: string;
  price: number;
};
const Cars = () => {
  const { keycloak } = useKeycloak();
  const [cars, setCars] = useState<Car[]>([]);
  useEffect(() => {
    const getData = async () => {
      try {
        if (keycloak && keycloak.authenticated) {
          await keycloak?.updateToken();
          const req = await fetch("http://localhost:1291/cars", {
            headers: {
              ["Authorization"]: `Bearer ${keycloak.token}`,
            },
          });
          setCars(await req.json());
        }
      } catch (e) {
        console.log("ERROR", e);
      }
    };
    getData();
  }, [keycloak]);
  return (
    <>
      <div style={{ marginTop: "20px" }}>
        {cars.map((car) => (
          <div key={car.name} style={{ padding: "10px", marginBottom: "20px" }}>
            <span>
              {car.name} - {car.color} | price: {car.price}
            </span>
          </div>
        ))}
      </div>
      <button
        type="button"
        className="text-blue-800"
        onClick={() => keycloak.logout()}
      >
        Logout ({keycloak?.tokenParsed?.preferred_username})
      </button>
    </>
  );
};
export default Cars;