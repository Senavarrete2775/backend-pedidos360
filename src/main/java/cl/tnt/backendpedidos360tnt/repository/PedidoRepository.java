package cl.tnt.backendpedidos360tnt.repository;

import cl.tnt.backendpedidos360tnt.model.Pedido;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PedidoRepository extends JpaRepository<Pedido, Long> {
}
