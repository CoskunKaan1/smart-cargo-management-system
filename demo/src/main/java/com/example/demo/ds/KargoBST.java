package com.example.demo.ds;


import com.example.demo.model.Kargo;
import java.util.ArrayList;
import java.util.List;

/**
 * AVL Ağacı (Dengeli BST) - Kargoları alıcı adına göre sıralı tutar.
 * In-order traversal ile alfabetik sıralı liste verir.
 */
public class KargoBST {

    private static class Node {
        Kargo kargo;
        Node left, right;
        int height;

        Node(Kargo kargo) {
            this.kargo = kargo;
            this.height = 1;
        }
    }

    private Node root;

    private int height(Node n) { return n == null ? 0 : n.height; }
    private int bf(Node n)     { return n == null ? 0 : height(n.left) - height(n.right); }

    private Node rotateRight(Node y) {
        Node x = y.left, T2 = x.right;
        x.right = y; y.left = T2;
        y.height = Math.max(height(y.left), height(y.right)) + 1;
        x.height = Math.max(height(x.left), height(x.right)) + 1;
        return x;
    }

    private Node rotateLeft(Node x) {
        Node y = x.right, T2 = y.left;
        y.left = x; x.right = T2;
        x.height = Math.max(height(x.left), height(x.right)) + 1;
        y.height = Math.max(height(y.left), height(y.right)) + 1;
        return y;
    }

    private Node insert(Node node, Kargo kargo) {
        if (node == null) return new Node(kargo);
        int cmp = kargo.getAlici().compareToIgnoreCase(node.kargo.getAlici());
        if (cmp < 0)       node.left  = insert(node.left,  kargo);
        else if (cmp > 0)  node.right = insert(node.right, kargo);
        else               node.right = insert(node.right, kargo); // Aynı isim: sağa ekle

        node.height = Math.max(height(node.left), height(node.right)) + 1;
        int balance = bf(node);

        if (balance > 1 && kargo.getAlici().compareToIgnoreCase(node.left.kargo.getAlici()) < 0)
            return rotateRight(node);
        if (balance < -1 && kargo.getAlici().compareToIgnoreCase(node.right.kargo.getAlici()) > 0)
            return rotateLeft(node);
        if (balance > 1 && kargo.getAlici().compareToIgnoreCase(node.left.kargo.getAlici()) > 0) {
            node.left = rotateLeft(node.left);
            return rotateRight(node);
        }
        if (balance < -1 && kargo.getAlici().compareToIgnoreCase(node.right.kargo.getAlici()) < 0) {
            node.right = rotateRight(node.right);
            return rotateLeft(node);
        }
        return node;
    }

    public void ekle(Kargo kargo) {
        root = insert(root, kargo);
    }

    private void inorder(Node node, List<Kargo> list) {
        if (node == null) return;
        inorder(node.left, list);
        list.add(node.kargo);
        inorder(node.right, list);
    }

    /** In-order traversal: alıcı adına göre alfabetik sıralı liste */
    public List<Kargo> siraliListe() {
        List<Kargo> list = new ArrayList<>();
        inorder(root, list);
        return list;
    }

    public void temizle() { root = null; }
}

