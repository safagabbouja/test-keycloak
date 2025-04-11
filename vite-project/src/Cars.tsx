import { useKeycloak } from "@react-keycloak/web";
import { useEffect, useState } from "react";

type Car = {
  id?: number; // Ajout de l'ID pour les opérations de mise à jour et de suppression
  name: string;
  color: string;
  price: number;
};

const Cars = () => {
  const { keycloak } = useKeycloak();
  const [cars, setCars] = useState<Car[]>([]);
  const [newCar, setNewCar] = useState<Car>({ name: "", color: "", price: 0 });
  const [editCar, setEditCar] = useState<Car | null>(null);

  // Récupérer toutes les voitures
  const fetchCars = async () => {
    try {
      if (keycloak && keycloak.authenticated) {
        await keycloak.updateToken();
        const response = await fetch("http://localhost:1291/api/cars", {
          headers: {
            Authorization: `Bearer ${keycloak.token}`,
          },
        });
        const data = await response.json();
        setCars(data);
      }
    } catch (error) {
      console.error("Error fetching cars:", error);
    }
  };

  // Créer une nouvelle voiture
  const createCar = async () => {
    try {
      if (keycloak && keycloak.authenticated) {
        await keycloak.updateToken();
        const response = await fetch("http://localhost:1291/api/cars", {
          method: "POST",
          headers: {
            "Content-Type": "application/json",
            Authorization: `Bearer ${keycloak.token}`,
          },
          body: JSON.stringify(newCar),
        });
        if (response.ok) {
          setNewCar({ name: "", color: "", price: 0 }); // Réinitialiser le formulaire
          fetchCars(); // Rafraîchir la liste des voitures
        }
      }
    } catch (error) {
      console.error("Error creating car:", error);
    }
  };

  // Mettre à jour une voiture existante
  const updateCar = async () => {
    if (!editCar || !editCar.id) return;

    try {
      if (keycloak && keycloak.authenticated) {
        await keycloak.updateToken();
        const response = await fetch(`http://localhost:1291/api/cars/${editCar.id}`, {
          method: "PUT",
          headers: {
            "Content-Type": "application/json",
            Authorization: `Bearer ${keycloak.token}`,
          },
          body: JSON.stringify(editCar),
        });
        if (response.ok) {
          setEditCar(null); // Fermer le formulaire de mise à jour
          fetchCars(); // Rafraîchir la liste des voitures
        }
      }
    } catch (error) {
      console.error("Error updating car:", error);
    }
  };

  // Supprimer une voiture
  const deleteCar = async (id: number) => {
    try {
      if (keycloak && keycloak.authenticated) {
        await keycloak.updateToken();
        const response = await fetch(`http://localhost:1291/api/cars/${id}`, {
          method: "DELETE",
          headers: {
            Authorization: `Bearer ${keycloak.token}`,
          },
        });
        if (response.ok) {
          fetchCars(); // Rafraîchir la liste des voitures
        }
      }
    } catch (error) {
      console.error("Error deleting car:", error);
    }
  };

  // Charger les voitures au montage du composant
  useEffect(() => {
    fetchCars();
  }, [keycloak]);

  return (
    <>
      <h3>Liste des voitures</h3>
      <div style={{ marginTop: "20px" }}>
        {cars.map((car) => (
          <div key={car.id} style={{ padding: "10px", marginBottom: "20px", border: "1px solid #ccc" }}>
            <span>
              {car.name} - {car.color} | Prix: {car.price}
            </span>
            <button onClick={() => setEditCar(car)}>Modifier</button>
            <button onClick={() => deleteCar(car.id!)}>Supprimer</button>
          </div>
        ))}
      </div>

      <h3>Ajouter une nouvelle voiture</h3>
      <div>
        <input
          type="text"
          placeholder="Nom"
          value={newCar.name}
          onChange={(e) => setNewCar({ ...newCar, name: e.target.value })}
        />
        <input
          type="text"
          placeholder="Couleur"
          value={newCar.color}
          onChange={(e) => setNewCar({ ...newCar, color: e.target.value })}
        />
        <input
          type="number"
          placeholder="Prix"
          value={newCar.price}
          onChange={(e) => setNewCar({ ...newCar, price: Number(e.target.value) })}
        />
        <button onClick={createCar}>Ajouter</button>
      </div>

      {editCar && (
        <>
          <h3>Modifier la voiture</h3>
          <div>
            <input
              type="text"
              placeholder="Nom"
              value={editCar.name}
              onChange={(e) => setEditCar({ ...editCar, name: e.target.value })}
            />
            <input
              type="text"
              placeholder="Couleur"
              value={editCar.color}
              onChange={(e) => setEditCar({ ...editCar, color: e.target.value })}
            />
            <input
              type="number"
              placeholder="Prix"
              value={editCar.price}
              onChange={(e) => setEditCar({ ...editCar, price: Number(e.target.value) })}
            />
            <button onClick={updateCar}>Enregistrer</button>
            <button onClick={() => setEditCar(null)}>Annuler</button>
          </div>
        </>
      )}

      <button
        type="button"
        className="text-blue-800"
        onClick={() => keycloak.logout()}
      >
        Déconnexion ({keycloak?.tokenParsed?.preferred_username})
      </button>
    </>
  );
};

export default Cars;