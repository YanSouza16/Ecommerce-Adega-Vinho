package com.example.pi.service;

import java.util.Date;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.example.pi.model.Cliente;
import com.example.pi.model.Compra;
import com.example.pi.model.Compra.StatusCompra;
import com.example.pi.model.CompraDTO;
import com.example.pi.model.CompraItem;
import com.example.pi.model.CompraItemDTO;
import com.example.pi.model.Endereco;
import com.example.pi.model.Produto;
import com.example.pi.repository.ClienteRepository;
import com.example.pi.repository.CompraItemRepository;
import com.example.pi.repository.CompraRepository;
import com.example.pi.repository.EnderecoRepository;
import com.example.pi.repository.ProdutoRepository;

import jakarta.transaction.Transactional;

@Service
public class CompraService {

    @Autowired
    private CompraRepository compraRepository;

    @Autowired
    private CompraItemRepository compraItemRepository;

    @Autowired
    private ClienteRepository clienteRepository;

    @Autowired
    private ProdutoRepository produtoRepository;

    @Autowired
    private EnderecoRepository enderecoRepository;

    @Transactional
    public Compra finalizarCompra(CompraDTO compraDTO) {
        // Criar a compra
        Compra compra = new Compra();
        // setar o json da compra para o insert no banco de dados
        compra.setNumeroPedido(compraDTO.getNumeroPedido());
        compra.setDataPedido(new Date());
        compra.setValorTotal(compraDTO.getValorTotal());
        compra.setStatus(Compra.StatusCompra.valueOf(compraDTO.getStatus()));
        compra.setTipoFrete(compraDTO.getTipoFrete());
        compra.setFormaPagamento(compraDTO.getFormaPagamento());
        compra.setParcelas(compraDTO.getParcelas());
        compra.setValorParcelas(compraDTO.getValorParcelas());
        // checar se o cliente existe
        Cliente cliente = clienteRepository.findById(compraDTO.getIdCliente())
                .orElseThrow(() -> new RuntimeException("Cliente não encontrado"));
        compra.setCliente(cliente);
        // checar se o endereco existe
        Endereco endereco = enderecoRepository.findById(compraDTO.getIdEndereco())
                .orElseThrow(() -> new RuntimeException("Endereço não encontrado"));
        compra.setEndereco(endereco);

        // chama o repositorio salva no banco e cria uma resposta para a api
        Compra compraSalva = compraRepository.save(compra);
        for (CompraItemDTO itemDTO : compraDTO.getItens()) {
            // verifica se os produtos da compra existem e cadastram
            Produto produto = produtoRepository.findById(itemDTO.getIdProduto())
                    .orElseThrow(() -> new RuntimeException("Produto não encontrado"));
            // verifica se tem a quantidade no estoque
            if (produto.getQtd() < itemDTO.getQuantidade()) {
                throw new RuntimeException("Quantidade solicitada excede o estoque disponível");
            }
            CompraItem item = new CompraItem();
            item.setCompra(compraSalva);
            item.setProduto(produto);
            item.setQuantidade(itemDTO.getQuantidade());
            compraItemRepository.save(item);
        }

        // retorna a compra 
        return compraSalva;
    }

    public void alterarStatus(Long idCompra, StatusCompra status) {
        compraRepository.updateStatusById(idCompra, status);
    }

    public List<Produto> getProdutosCarrinho() {
        return produtoRepository.findAll();
    }

    public Double calcularTotal(List<Produto> produtos) {
        double total = 0;
        for (Produto produto : produtos) {
            total += produto.getPreco();
        }
        return total;
    }
}
