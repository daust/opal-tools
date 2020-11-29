package de.opal.utils;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import de.opal.installer.PatchFileMapping;

public class ListUtils {

	Node head1;
	Node head2;

	private class Node {
		int data;
		Node next;

		Node(int x) {
			data = x;
		}
	}

	public static Node mergeLists(Node headA, Node headB) {
		Node headC = null;
		if (headA == null) {
			return headB;
		}
		if (headB == null) {
			return headA;
		}

		if (headA.data <= headB.data) {
			headC = headA;
			headC.next = mergeLists(headA.next, headB);
		} else {
			headC = headB;
			headC.next = mergeLists(headA, headB.next);
		}
		return headC;
	}

	public void display(Node head) {
		Node temp = head;

		while (temp != null) {
			System.out.print(temp.data + " ");
			temp = temp.next;
		}
		System.out.println();
	}

	public static void main(String[] args) {
		ListUtils obj = new ListUtils();

		obj.head1 = obj.new Node(1);
		obj.head1.next = obj.new Node(3);
		obj.head2 = obj.new Node(2);
		obj.head2.next = obj.new Node(4);
		obj.display(obj.head1);
		obj.display(obj.head2);
		obj.display(obj.mergeLists(obj.head1, obj.head2));

	}

	public static List<PatchFileMapping> mergeWithIterator(List<PatchFileMapping> list1, List<PatchFileMapping> list2) {
		List<PatchFileMapping> sorted = new ArrayList<PatchFileMapping>();

		Iterator<PatchFileMapping> iterator1 = list1.iterator();
		Iterator<PatchFileMapping> iterator2 = list2.iterator();
		PatchFileMapping element1 = null;
		PatchFileMapping element2 = null;

		while (iterator1.hasNext() && iterator2.hasNext()) {
			if (element1 == null) {
				element1 = iterator1.next();
			}
			if (element2 == null) {
				element2 = iterator2.next();
			}

			if (element1.compareTo(element2) < 0) {
				sorted.add(element1);
				element1 = null;
			} else if (element1.compareTo(element2) > 0) {
				sorted.add(element2);
				element2 = null;
			} else {
				sorted.add(element1);
				element1 = null;
				element2 = null;
			}
		}

		if (element1 != null) {
			sorted.add(element1);
		}
		if (element2 != null) {
			sorted.add(element2);
		}

		while (iterator1.hasNext()) {
			sorted.add(iterator1.next());
		}
		while (iterator2.hasNext()) {
			sorted.add(iterator2.next());
		}

		return sorted;
	}

}
